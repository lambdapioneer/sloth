package com.lambdapioneer.sloth.impl

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.SlothInconsistentState
import com.lambdapioneer.sloth.SlothStorageKeyNotFound
import com.lambdapioneer.sloth.crypto.Hkdf
import com.lambdapioneer.sloth.crypto.HmacAlgorithm
import com.lambdapioneer.sloth.crypto.PwHash
import com.lambdapioneer.sloth.impl.LongSlothKeys.*
import com.lambdapioneer.sloth.secureelement.KeyHandle
import com.lambdapioneer.sloth.secureelement.SecureElement
import com.lambdapioneer.sloth.storage.ReadableStorage
import com.lambdapioneer.sloth.storage.WriteableStorage
import com.lambdapioneer.sloth.utils.NoopTracer
import com.lambdapioneer.sloth.utils.Tracer
import com.lambdapioneer.sloth.utils.secureRandomBytes
import com.lambdapioneer.sloth.utils.secureRandomChars

/**
 * Keys for the values persisted in storage.
 */
internal enum class LongSlothKeys(val key: String) {
    /**
     * The handle under which the secret key is stored in the Secure Element
     */
    H("h"),

    /**
     * The salt used for both the password hashing (outside the Secure Element) and the
     * HMAC key derivation (inside the Secure Element).
     */
    SALT("salt"),
}

@VisibleForTesting
@RequiresApi(Build.VERSION_CODES.P)
class LongSlothImpl(
    private val params: LongSlothParams,
    private val secureElement: SecureElement,
    private val pwHash: PwHash,
    private val tracer: Tracer = NoopTracer(),
) {
    private val hkdf = Hkdf(HmacAlgorithm.SHA256)

    fun onAppStart(storage: WriteableStorage, h: ByteArray) {
        if (exists(storage)) {
            // if the storage already exists, we update the last modified timestamps
            storage.updateAllLastModifiedTimestamps()
        } else {
            // if the storage does not exist, we create a new key under a randomly chosen passphrase
            val randomPassphrase = secureRandomChars(entropy = params.lambda.toDouble())
            keyGen(
                storage = storage,
                pw = randomPassphrase,
                h = h,
                outputLengthBytes = 32, // this value is arbitrary
            )
        }
    }

    fun keyGen(
        storage: WriteableStorage,
        pw: CharArray,
        h: ByteArray,
        outputLengthBytes: Int,
    ): ByteArray {
        storage.put(H.key, h)
        storage.put(SALT.key, pwHash.createSalt())

        secureElement.hmacGenKey(keyHandle = KeyHandle(h))

        return derive(storage, pw, outputLengthBytes)
    }

    fun derive(storage: ReadableStorage, pw: CharArray, outputLengthBytes: Int): ByteArray {
        tracer.addKeyValue("l", params.l.toString())
        tracer.addKeyValue("argon", pwHash.toString())

        tracer.start()

        val omegaPre = pwHash.deriveHash(
            salt = storage.get(SALT.key),
            password = pw,
            outputLengthInBytes = params.l,
        )

        tracer.step("afterPwHash")

        val omegaPost = secureElement.hmacDerive(
            keyHandle = KeyHandle(storage.get(H.key)),
            data = omegaPre
        )

        tracer.step("afterSeHmac")

        val k = hkdf.derive(
            salt = storage.get(SALT.key),
            ikm = omegaPost,
            info = HKDF_INFO_CONSTANT.encodeToByteArray(),
            l = outputLengthBytes
        )

        tracer.finish()

        return k
    }

    fun exists(storage: ReadableStorage): Boolean {
        val exists = LongSlothKeys.values().map {
            try {
                storage.get(it.key).isNotEmpty()
            } catch (e: SlothStorageKeyNotFound) {
                return false
            }
        }

        // if some keys exists, while others are not, then that indicates
        // that we are in an inconsistent state
        if (exists.any() && !exists.all { it }) {
            throw SlothInconsistentState("Some LongSloth files are missing.")
        }

        return true
    }

    companion object {
        internal const val HKDF_INFO_CONSTANT = "LongSlothHkdfInfoConstant"
    }
}
