package com.lambdapioneer.sloth.impl

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.SlothInconsistentState
import com.lambdapioneer.sloth.SlothStorageKeyNotFound
import com.lambdapioneer.sloth.crypto.AuthenticatedEncryption
import com.lambdapioneer.sloth.crypto.PwHash
import com.lambdapioneer.sloth.impl.HiddenSlothKeys.*
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

/**
 * Keys for the values persisted in storage.
 */
internal enum class HiddenSlothKeys(val key: String) {
    /**
     * The handle under which the secret AES key for the outer ciphertext is stored inside the
     * Secure Element
     */
    OUTER_H("outer_h"),

    /**
     * The IV used for wrapping the outer ciphertext secrets [TK] and [TIV]
     */
    OUTER_IV("outer_iv"),

    /**
     * The encrypted AES key for the outer ciphertext
     */
    TK("tk"),

    /**
     * The encrypted AES IV for the outer ciphertext
     */
    TIV("tiv"),

    /**
     * The IV used for the inner ciphertext
     */
    INNER_IV("iv"),

    /**
     * The encrypted outer ciphertext which encrypts the inner ciphertext
     */
    BLOB("blob"),
}

@VisibleForTesting
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

    fun onAppStart(storage: WriteableStorage, h: ByteArray) {
        if (exists(storage)) {
            // if the storage already exists, we update the last modified timestamps
            storage.updateAllLastModifiedTimestamps()
        } else {
            // if the storage does not exist, we create a new key under a randomly chosen passphrase
            init(storage, h)
        }

        val hDess = storage.get(LongSlothKeys.H.key)
        longSloth.onAppStart(storage, hDess)
    }

    fun init(storage: WriteableStorage, h: ByteArray, tracer: Tracer = NoopTracer()) {
        tracer.start()

        val hDess = h + "dess".toByteArray()
        dessInit(storage, hDess)

        val hDems = h + "dems".toByteArray()
        storage.put(OUTER_H.key, hDems)

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

        return true
    }

    /**
     * Encrypts the given [data] using the password [pw] and stores the ciphertext in [storage].
     * If [cachedSecrets] is provided, the encryption is performed using the cached secrets instead
     * of using the password.
     */
    fun encrypt(
        storage: WriteableStorage,
        pw: String?,
        data: ByteArray,
        tracer: Tracer = NoopTracer(),
        cachedSecrets: HiddenSlothCachedSecrets? = null,
    ) {
        tracer.start()
        val hasPassword = pw != null
        val hasCachedSecrets = cachedSecrets != null
        require(hasPassword xor hasCachedSecrets) { "Either pw or cachedSecrets must be provided" }
        require(data.size <= params.payloadMaxLength) { "payload too large" }

        //
        // (1) Encrypt the inner data using the single-snapshot scheme
        //
        val dessEncryptionResult = dessEncrypt(storage, pw, data, cachedSecrets)

        //
        // (2) Encrypt the inner ciphertext in another layer of encryption using temporary secrets
        // `tk` and `tiv`. This later allows us to ratchet the outer-layer without modifying the
        // inner layer.
        //
        val tk = AuthenticatedEncryption.keyGen()
        val tiv = AuthenticatedEncryption.ivGen()
        val encryptedBlobAndTag = AuthenticatedEncryption.encrypt(
            k = tk,
            iv = tiv,
            data = dessEncryptionResult.blobAndTag
        )
        storage.put(BLOB.key, encryptedBlobAndTag)

        //
        // (3) Encrypt the secrets `tk` and `tiv` using an outer layer of encryption using the
        // a new key inside the secure element referenced under `hDems`.
        //
        val hDems = KeyHandle(storage.get(OUTER_H.key))
        secureElement.aesCtrGenKey(hDems)

        val seIv = secureElement.aesCtrGenIv()
        storage.put(OUTER_IV.key, seIv)

        // unrolled for loop (the `tags` are part of the cipher text in this implementation)
        storage.put(INNER_IV.key, secureElement.aesCtrEncrypt(hDems, seIv, dessEncryptionResult.iv))
        storage.put(TK.key, secureElement.aesCtrEncrypt(hDems, seIv, tk))
        storage.put(TIV.key, secureElement.aesCtrEncrypt(hDems, seIv, tiv))

        tracer.finish()
    }

    /**
     * Authenticates the ciphertext stored in [storage] using key stored in the secure element. This
     * simply checks the outer layer of encryption and therefore it is not dependent of the user
     * passphrase and does not leak any information about the presence of any meaningful encrypted
     * data.
     *
     * If the storage blob does not authenticate, an [AEADBadTagException] is thrown.
     */
    @Throws(AEADBadTagException::class)
    fun authenticate(storage: ReadableStorage) {
        //
        // (1) Decrypt the `tk` secret
        //
        val hDems = KeyHandle(storage.get(OUTER_H.key))
        val seIv = storage.get(OUTER_IV.key)
        val tk = secureElement.aesCtrDecrypt(hDems, seIv, storage.get(TK.key))

        //
        // (2) Authenticate the outer ciphertext
        //
        AuthenticatedEncryption.authenticate(tk, storage.get(BLOB.key))
    }

    /**
     * Prepares [HiddenSlothCachedSecrets] that can be used with [#decrypt] to speed up repeated access to
     * the ciphertext.
     */
    fun computeCachedSecrets(storage: ReadableStorage, pw: String): HiddenSlothCachedSecrets {
        val k = dessDeriveKey(storage, pw)
        return HiddenSlothCachedSecrets(k = k)
    }

    /**
     * Decrypts the ciphertext stored in [storage] using the password [pw]. If [cachedSecrets] is
     * provided, the decryption is performed using the cached secrets. Otherwise, the secrets are
     * derived from the password. Exactly one of them must be provided.
     *
     * If [decryptionOffsetAndLength] is provided, only the ciphertext in the range specified by
     * [decryptionOffsetAndLength] is decrypted. Note that in this case the decryption is not
     * authenticated. The offset and length must be aligned at AES block boundaries (16 bytes).
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
            val hDems = KeyHandle(storage.get(OUTER_H.key))
            val seIv = storage.get(OUTER_IV.key)

            //
            // (1) Decrypt the `iv`, `tk` and `tiv` secrets for the outer ciphertext
            //
            val iv = secureElement.aesCtrDecrypt(
                keyHandle = hDems,
                iv = seIv,
                data = storage.get(INNER_IV.key)
            )
            val tk = secureElement.aesCtrDecrypt(
                keyHandle = hDems,
                iv = seIv,
                data = storage.get(TK.key)
            )
            val tiv = secureElement.aesCtrDecrypt(
                keyHandle = hDems,
                iv = seIv,
                data = storage.get(TIV.key)
            )

            //
            // (2) Decrypt the outer ciphertext using the `tk` and `tiv` secrets
            //
            val blobAndTag = AuthenticatedEncryption.decrypt(
                k = tk,
                iv = tiv,
                ciphertextAndTag = storage.get(BLOB.key)
            )

            //
            // (3) Decrypt the inner ciphertext using the password-derived key (or `k` from the
            // cached secrets)
            //
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

        val hDems = KeyHandle(storage.get(OUTER_H.key))
        val seIv = storage.get(OUTER_IV.key)

        //
        // (1) Decrypt the `iv`, `tk` and `tiv` secrets for the outer ciphertext
        //
        val iv = secureElement.aesCtrDecrypt(hDems, seIv, storage.get(INNER_IV.key))
        val tk = secureElement.aesCtrDecrypt(hDems, seIv, storage.get(TK.key))
        val tiv = secureElement.aesCtrDecrypt(hDems, seIv, storage.get(TIV.key))

        //
        // (2) Create new `tk` and `tiv` secrets for the outer ciphertext
        //
        val newTk = AuthenticatedEncryption.keyGen()
        val newTiv = AuthenticatedEncryption.ivGen()

        //
        // (3) Re-encrypt the outer ciphertext using the new `tk` and `tiv` secrets
        //
        val dataAndTag = storage.get(BLOB.key)
        AuthenticatedEncryption.inplaceDecryptEncrypt(
            dataAndTag = dataAndTag,
            decryptK = tk,
            decryptIv = tiv,
            encryptK = newTk,
            encryptIv = newTiv,
        )
        storage.put(BLOB.key, dataAndTag)

        //
        // (4) Re-encrypt the new `iv`, `tk` and `tiv` secrets for the outer ciphertext using a
        // fresh key inside the secure element.
        //
        secureElement.aesCtrGenKey(hDems)

        val newSeIv = secureElement.aesCtrGenIv()
        storage.put(OUTER_IV.key, newSeIv)

        storage.put(INNER_IV.key, secureElement.aesCtrEncrypt(hDems, newSeIv, iv))
        storage.put(TK.key, secureElement.aesCtrEncrypt(hDems, newSeIv, newTk))
        storage.put(TIV.key, secureElement.aesCtrEncrypt(hDems, newSeIv, newTiv))

        tracer.finish()
    }

    //
    // All DESS methods
    //

    /**
     * Initializes the DESS scheme. This method must be called before any other DESS method.
     */
    private fun dessInit(storage: WriteableStorage, h: ByteArray) {
        val pw = secureRandomBytes(params.lambda).decodeToString()

        @Suppress("UNUSED_VARIABLE")
        val k = longSloth.keyGen(storage, pw, h, slothKeyLenInBytes)

        dessEncrypt(storage = storage, pw = pw, data = ByteArray(0), cachedSecrets = null)
    }

    /**
     * Derives the DESS key from the password [pw] using the LongSloth scheme. There should be no
     * (meaningful) authentication of the inner ciphertext before this method is called.
     */
    private fun dessDeriveKey(
        storage: ReadableStorage,
        pw: String,
    ) = longSloth.derive(storage = storage, pw = pw, outputLengthBytes = slothKeyLenInBytes)

    @Suppress("ArrayInDataClass")
    data class DessEncryptionResult(val iv: ByteArray, val blobAndTag: ByteArray)

    /**
     * Encrypts the given [data] using the password [pw] and returns the ciphertext and the IV.
     * Note that the storage is read-only and the ciphertext is not stored in the storage.
     */
    private fun dessEncrypt(
        storage: ReadableStorage,
        pw: String?,
        data: ByteArray,
        cachedSecrets: HiddenSlothCachedSecrets?,
    ): DessEncryptionResult {
        // caller enforces that either `cachedSecrets` or `pw` is not null
        val k = cachedSecrets?.k ?: dessDeriveKey(storage, pw!!)

        val content = ByteBuffer.allocate(params.payloadMaxLength + contentOverhead())
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

    /**
     * Decrypts the given [blobAndTag] using the password [pw] and returns the plaintext. Note that
     * the storage is read-only and will not be updated.
     *
     * If [cachedSecrets] is provided, the decryption is performed using the cached secrets.
     *
     * If [decryptionOffsetAndLength] is provided, only the ciphertext in the range specified by
     * [decryptionOffsetAndLength] is decrypted. Note that in this case the decryption is not
     * authenticated. The offset and length must be aligned at AES block boundaries (16 bytes, see
     * [AuthenticatedEncryption.BLOCK_LEN]).
     */
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

        val offsetPastLenFieldIncludingPadding = contentOverhead()
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

    companion object {
        // The ciphertext starts with 4 Bytes for the payload size. This information is then padded to
        // the next AES block boundary. This makes offsets into the ciphertext block aligned.
        private const val PAYLOAD_SIZE_FIELD_LEN = Int.SIZE_BYTES
        private const val PAYLOAD_SIZE_FIELD_PADDING =
            AuthenticatedEncryption.BLOCK_LEN - PAYLOAD_SIZE_FIELD_LEN

        /**
         * The total overhead in bytes for the content. This includes the payload size field and the
         * padding.
         */
        fun contentOverhead(): Int {
            return PAYLOAD_SIZE_FIELD_LEN + PAYLOAD_SIZE_FIELD_PADDING
        }

        /**
         * The total ciphertext overhead in bytes. This includes the contentOverhead and two tags
         * from the authenticated encryption. One is for the outer ciphertext (enabling the ratchet
         * operation) and one is for the inner ciphertext (authenticating the data and verifying
         * the password).
         */
        fun ciphertextTotalOverhead(): Int {
            return contentOverhead() + 2 * AuthenticatedEncryption.TAG_LEN
        }
    }
}
