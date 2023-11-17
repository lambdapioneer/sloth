package com.lambdapioneer.sloth.impl

import android.os.Build
import androidx.annotation.RequiresApi
import com.lambdapioneer.sloth.SlothInconsistentState
import com.lambdapioneer.sloth.SlothStorageKeyNotFound
import com.lambdapioneer.sloth.crypto.AuthenticatedEncryption
import com.lambdapioneer.sloth.crypto.PwHash
import com.lambdapioneer.sloth.secureelement.KeyHandle
import com.lambdapioneer.sloth.secureelement.SecureElement
import com.lambdapioneer.sloth.storage.ReadableStorage
import com.lambdapioneer.sloth.storage.WriteableStorage
import com.lambdapioneer.sloth.utils.NoopTracer
import com.lambdapioneer.sloth.utils.Tracer
import com.lambdapioneer.sloth.utils.ceilOfIntegerDivision
import com.lambdapioneer.sloth.utils.secureRandomBytes
import java.nio.ByteBuffer
import java.util.*
import javax.crypto.AEADBadTagException

enum class HiddenSlothKeys(val key: String) {
    H_DEMS("hDems"),
    SE_IV("seIv"),
    IV("iv"),
    TK("tk"),
    BLOB("blob"),
    TIV("tiv"),
}

@RequiresApi(Build.VERSION_CODES.P)
class HiddenSlothImpl(
    private val params: HiddenSlothParams,
    private val secureElement: SecureElement,
    pwHash: PwHash,
    tracer: Tracer = NoopTracer(),
) {
    // Lambda is given in bits, but we need bytes for the key length
    private val slothKeyLenInBytes = ceilOfIntegerDivision(params.lambda, 8)

    private val longSloth = LongSlothImpl(
        params = params.longSlothParams,
        secureElement = secureElement,
        pwHash = pwHash,
        tracer = tracer
    )

    fun init(storage: WriteableStorage, h: ByteArray, tracer: Tracer = NoopTracer()) {
        tracer.start()

        val hDess = h + "_dess".encodeToByteArray()
        dessInit(storage, hDess)

        val hDems = h + "_dems".encodeToByteArray()
        storage.put("hDems", hDems)

        secureElement.aesCtrGenKey(KeyHandle(hDems))

        val pw = secureRandomBytes(slothKeyLenInBytes).decodeToString()
        encrypt(storage, pw, ByteArray(0))

        tracer.finish()
    }

    fun exists(storage: ReadableStorage): Boolean {
        val exists = HiddenSlothKeys.values().map {
            try {
                storage.get(it.key).isNotEmpty()
            } catch (e: SlothStorageKeyNotFound) {
                return false
            }
        }

        // if some keys exists, while others are not, then that indicates
        // that we are in an inconsistent state
        if (exists.any() && !exists.all { it }) {
            throw SlothInconsistentState("Some HiddenSloth files are missing.")
        }

        return exists.any()
    }

    fun maxPayloadSize() =
        params.storageTotalSize - PAYLOAD_SIZE_FIELD_LEN - PAYLOAD_SIZE_FIELD_PADDING

    fun encrypt(
        storage: WriteableStorage,
        pw: String,
        data: ByteArray,
        tracer: Tracer = NoopTracer(),
    ) {
        tracer.start()

        require(data.size <= maxPayloadSize())
        val hDems = KeyHandle(storage.get("hDems"))

        val dessEncryptionResult = dessEncrypt(storage, pw, data)

        val tk = AuthenticatedEncryption.keyGen()
        val tiv = AuthenticatedEncryption.ivGen()
        val encryptedBlobAndTag =
            AuthenticatedEncryption.encrypt(tk, tiv, dessEncryptionResult.blobAndTag)
        storage.put("blob", encryptedBlobAndTag)

        val seIv = secureElement.aesCtrGenIv()
        storage.put("seIv", seIv)

        // unrolled for loop (the `tags` are part of the cipher text in this implementation)
        storage.put("iv", secureElement.aesCtrEncrypt(hDems, seIv, dessEncryptionResult.iv))
        storage.put("tk", secureElement.aesCtrEncrypt(hDems, seIv, tk))
        storage.put("tiv", secureElement.aesCtrEncrypt(hDems, seIv, tiv))

        tracer.finish()
    }

    /**
     * Authenticates the ciphertext stored in [storage] key stored in the secure element. If the
     * storage blob does not authenticate, an [AEADBadTagException] is thrown.
     */
    @Throws(AEADBadTagException::class)
    fun authenticate(storage: ReadableStorage) {
        val hDems = KeyHandle(storage.get("hDems"))
        val seIv = storage.get("seIv")

        val tk = secureElement.aesCtrDecrypt(hDems, seIv, storage.get("tk"))
        AuthenticatedEncryption.authenticate(tk, storage.get("blob"))
    }

    /**
     * Prepares [HiddenSlothCachedSecrets] that can be used with [#decrypt] to speed up repeated access to
     * the ciphertext.
     */
    fun prepareCachedSecrets(storage: ReadableStorage, pw: String): HiddenSlothCachedSecrets {
        val hDems = KeyHandle(storage.get("hDems"))
        val seIv = storage.get("seIv")

        return HiddenSlothCachedSecrets(
            iv = secureElement.aesCtrDecrypt(hDems, seIv, storage.get("iv")),
            tk = secureElement.aesCtrDecrypt(hDems, seIv, storage.get("tk")),
            tiv = secureElement.aesCtrDecrypt(hDems, seIv, storage.get("tiv")),
            k = dessDeriveKey(storage, pw),
        )
    }

    /**
     * Decrypts the ciphertext stored in [storage] using the password [pw]. If [cachedSecrets] is
     * provided, the decryption is performed using the cached secrets. Otherwise, the secrets are
     * derived from the password. Exactly one of them must be provided.
     *
     * If [decryptionOffsetAndLength] is provided, only the ciphertext in the range specified by
     * [decryptionOffsetAndLength] is decrypted. Note that in this case the decryption is not
     * authenticated.The offset and length must be aligned at AES block
     * boundaries (16 bytes, see[AE_BLOCK_LEN]).
     */
    fun decrypt(
        storage: ReadableStorage,
        pw: String?,
        tracer: Tracer = NoopTracer(),
        cachedSecrets: HiddenSlothCachedSecrets? = null,
        decryptionOffsetAndLength: OffsetAndLength? = null,
    ): ByteArray {
        val hasPassword = pw != null
        val hasCachedSecrets = cachedSecrets != null
        require(hasPassword xor hasCachedSecrets) { "Either pw or cachedSecrets must be provided" }

        tracer.start()
        try {
            val hDems = KeyHandle(storage.get("hDems"))
            val seIv = storage.get("seIv")

            // unrolled for loop (the `tags` are part of the cipher text in this implementation)
            val iv =
                cachedSecrets?.iv ?: secureElement.aesCtrDecrypt(hDems, seIv, storage.get("iv"))
            val tk =
                cachedSecrets?.tk ?: secureElement.aesCtrDecrypt(hDems, seIv, storage.get("tk"))
            val tiv =
                cachedSecrets?.tiv ?: secureElement.aesCtrDecrypt(hDems, seIv, storage.get("tiv"))

            val blobAndTag = AuthenticatedEncryption.decrypt(tk, tiv, storage.get("blob"))

            return dessDecrypt(
                storage = storage,
                pw = pw,
                iv = iv,
                blobAndTag = blobAndTag,
                cachedSecrets = cachedSecrets,
                decryptionOffsetAndLength = decryptionOffsetAndLength,
            )
        } finally {
            tracer.finish()
        }
    }

    fun ratchet(storage: WriteableStorage, tracer: Tracer = NoopTracer()) {
        tracer.start()

        val hDems = KeyHandle(storage.get("hDems"))
        val seIv = storage.get("seIv")

        // unrolled for loop (the `tags` are part of the cipher text in this implementation)
        val iv = secureElement.aesCtrDecrypt(hDems, seIv, storage.get("iv"))
        val tk = secureElement.aesCtrDecrypt(hDems, seIv, storage.get("tk"))
        val tiv = secureElement.aesCtrDecrypt(hDems, seIv, storage.get("tiv"))

        // re-encrypt `blobAndTag` with new keys
        val newTk = AuthenticatedEncryption.keyGen()
        val newTiv = AuthenticatedEncryption.ivGen()

        val dataAndTag = storage.get("blob")
        AuthenticatedEncryption.inplaceDecryptEncrypt(
            dataAndTag = dataAndTag,
            decryptK = tk,
            decryptIv = tiv,
            encryptK = newTk,
            encryptIv = newTiv,
        )
        storage.put("blob", dataAndTag)

        secureElement.aesCtrGenKey(hDems)

        // unrolled for loop (the `tags` are part of the cipher text in this implementation)
        storage.put("iv", secureElement.aesCtrEncrypt(hDems, seIv, iv))
        storage.put("tk", secureElement.aesCtrEncrypt(hDems, seIv, newTk))
        storage.put("tiv", secureElement.aesCtrEncrypt(hDems, seIv, newTiv))

        tracer.finish()
    }

    //
    // All DESS methods
    //

    private fun dessInit(storage: WriteableStorage, h: ByteArray) {
        val pw = secureRandomBytes(params.lambda).decodeToString()

        @Suppress("UNUSED_VARIABLE")
        val k = longSloth.keyGen(storage, pw, h, slothKeyLenInBytes)

        dessEncrypt(storage, pw, ByteArray(0))
    }

    @Suppress("ArrayInDataClass")
    data class DessEncryptionResult(val iv: ByteArray, val blobAndTag: ByteArray)

    private fun dessEncrypt(
        storage: ReadableStorage,
        pw: String,
        data: ByteArray,
    ): DessEncryptionResult {
        val k = dessDeriveKey(storage, pw)

        // TODO: do we allocate too much here? Compare with `maxPayloadSize`
        val content =
            ByteBuffer.allocate(params.storageTotalSize + PAYLOAD_SIZE_FIELD_LEN + PAYLOAD_SIZE_FIELD_PADDING)
        content.putInt(data.size)
        content.put(ByteArray(PAYLOAD_SIZE_FIELD_PADDING))
        content.put(data)

        val iv = secureRandomBytes(AuthenticatedEncryption.IV_LEN)
        // return `blobAndTag` directly (different to the paper notation, but equivalent)
        return DessEncryptionResult(
            iv = iv,
            blobAndTag = AuthenticatedEncryption.encrypt(k, iv, content.array())
        )
    }

    @Suppress("UsePropertyAccessSyntax")
    private fun dessDecrypt(
        storage: ReadableStorage,
        pw: String?,
        iv: ByteArray,
        blobAndTag: ByteArray,
        cachedSecrets: HiddenSlothCachedSecrets? = null,
        decryptionOffsetAndLength: OffsetAndLength?,
    ): ByteArray {
        // caller enforces that either `cachedSecrets` or `pw` is not null
        val k = cachedSecrets?.k ?: dessDeriveKey(storage, pw!!)

        val offsetPastLenFieldIncludingPadding = PAYLOAD_SIZE_FIELD_LEN + PAYLOAD_SIZE_FIELD_PADDING
        if (decryptionOffsetAndLength != null) {
            // Trust the caller to provide a valid offset and length. We offset it into the overall
            // ciphertext by moving the offset past the field containing the payload size and the
            // padding. Since they add up to a multiple of the AES block size, we maintain the
            // relative block alignment.
            val offsetPastLenField =
                offsetPastLenFieldIncludingPadding + decryptionOffsetAndLength.offset

            return AuthenticatedEncryption.decryptUnauthenticated(
                k = k,
                iv = iv,
                dataAndTag = blobAndTag,
                offset = offsetPastLenField,
                length = decryptionOffsetAndLength.length
            )
        } else {
            // Decrypt the full ciphertext and authenticate using the tag. Upon success use the
            // first 4 bytes of the decrypted plaintext as the payload size and return the
            // remaining bytes as the payload.
            val x = AuthenticatedEncryption.decrypt(k, iv, blobAndTag)
            val buffer = ByteBuffer.wrap(x)
            val payloadSize = buffer.getInt()
            return x.copyOfRange(
                offsetPastLenFieldIncludingPadding,
                offsetPastLenFieldIncludingPadding + payloadSize
            )
        }
    }

    private fun dessDeriveKey(
        storage: ReadableStorage,
        pw: String,
    ) = longSloth.derive(storage, pw, slothKeyLenInBytes)

    companion object {
        // The ciphertext starts with 4 Bytes for the payload size. This information is then padded to
        // the next AES block boundary. This makes offsets into the ciphertext block aligned.
        private const val PAYLOAD_SIZE_FIELD_LEN = Int.SIZE_BYTES
        private const val PAYLOAD_SIZE_FIELD_PADDING =
            AuthenticatedEncryption.BLOCK_LEN - PAYLOAD_SIZE_FIELD_LEN
    }
}
