package com.lambdapioneer.sloth.pwhash_libsodium

import com.goterl.lazysodium.SodiumAndroid
import com.lambdapioneer.sloth.crypto.PwHash
import com.lambdapioneer.sloth.utils.secureRandomBytes
import java.nio.CharBuffer
import java.util.Arrays
import com.goterl.lazysodium.interfaces.PwHash as LibSodiumPwHash


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
        password: CharArray,
        outputLengthInBytes: Int,
    ): ByteArray {
        require(salt.size == SALT_LEN)
        require(outputLengthInBytes > 0)
        require(password.isNotEmpty())

        // Wrapping creates no internal copy
        val passwordCharBuffer = CharBuffer.wrap(password)

        // The encode operation "should" be compatible with `String.encodeToByteArray()` and
        // returns an array-backed `ByteBuffer`. Hence, we avoid the intermediate step via
        // an immutable String that lingers in memory
        val passwordBytesBuffer = Charsets.UTF_8.encode(passwordCharBuffer)
        check(passwordBytesBuffer.hasArray())

        val passwordBytesArray = passwordBytesBuffer.array()
        val passwordBytesArrayLength = passwordBytesBuffer.limit()

        val result = ByteArray(outputLengthInBytes)
        try {
            val libSodiumResult = libSodium.crypto_pwhash(
                /* outputHash = */ result,
                /* outputHashLen = */ result.size.toLong(),
                /* password = */ passwordBytesArray,
                /* passwordLen = */ passwordBytesArrayLength.toLong(),
                /* salt = */ salt,
                /* opsLimit = */ params.opsLimit,
                /* memLimit = */ params.memLimit,
                /* alg = */ LibSodiumPwHash.Alg.PWHASH_ALG_ARGON2ID13.value,
            )
            check(libSodiumResult == 0)
        } finally {
            Arrays.fill(passwordBytesArray, 0x00)
        }

        return result
    }

    override fun toString(): String {
        return params.toString()
    }

    companion object {
        private const val SALT_LEN = LibSodiumPwHash.SALTBYTES
    }
}

