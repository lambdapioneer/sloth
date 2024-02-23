package com.lambdapioneer.sloth.crypto

import com.lambdapioneer.sloth.utils.secureRandomBytes
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * A simple password hashing implementation based on PBKDF2.
 */
class Pbkdf2PwHash(
    private val params: Pbkdf2PwHashParams = Pbkdf2PwHashParams.INTERACTIVE,
) : PwHash {

    override fun createSalt() = secureRandomBytes(SALT_LEN)

    override fun deriveHash(
        salt: ByteArray,
        password: CharArray,
        outputLengthInBytes: Int,
    ): ByteArray {
        val factory = SecretKeyFactory.getInstance(params.algorithm)
        val keySpec = PBEKeySpec(
            /* password = */ password,
            /* salt = */ salt,
            /* iterationCount = */ params.iterationCount,
            /* keyLength = */ 8 * outputLengthInBytes
        )
        val secret = factory.generateSecret(keySpec)!!

        val result = secret.encoded
        check(result.size == outputLengthInBytes)
        return result
    }

    companion object {
        private const val SALT_LEN = 32
    }
}
