package com.lambdapioneer.sloth.pwhash_libsodium

import com.goterl.lazysodium.SodiumAndroid
import com.lambdapioneer.sloth.crypto.PwHash
import com.lambdapioneer.sloth.utils.secureRandomBytes


class LibSodiumArgon2PwHash(
    private val params: LibSodiumArgon2PwHashParams = LibSodiumArgon2PwHashParams.INTERACTIVE,
) : PwHash {
    private val libSodium = SodiumAndroid()

    init {
        libSodium.sodium_init()
    }

    override fun createSalt() = secureRandomBytes(SALT_LEN)

    override fun deriveHash(
        salt: ByteArray,
        password: String,
        outputLengthInBytes: Int,
    ): ByteArray {
        require(salt.size == SALT_LEN)
        require(outputLengthInBytes > 0)
        require(password.isNotEmpty())

        val passwordBytes = password.encodeToByteArray()
        val result = ByteArray(outputLengthInBytes)

        val libSodiumResult = libSodium.crypto_pwhash(
            result, result.size.toLong(),
            passwordBytes, passwordBytes.size.toLong(),
            salt,
            params.opsLimit,
            params.memLimit,
            com.goterl.lazysodium.interfaces.PwHash.Alg.PWHASH_ALG_ARGON2ID13.value,
        )
        check(libSodiumResult == 0)

        return result
    }

    override fun toString(): String {
        return params.toString()
    }

    companion object {
        private const val SALT_LEN = com.goterl.lazysodium.interfaces.PwHash.SALTBYTES
    }
}

