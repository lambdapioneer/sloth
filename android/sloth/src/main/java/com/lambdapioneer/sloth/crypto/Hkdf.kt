package com.lambdapioneer.sloth.crypto

import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.utils.ceilOfIntegerDivision
import com.lambdapioneer.sloth.utils.concat
import java.nio.ByteBuffer
import java.security.spec.KeySpec
import javax.crypto.Mac
import javax.crypto.SecretKey


internal enum class HmacAlgorithm(val algorithm: String, val hashLen: Int) {
    SHA256("HmacSHA256", 256 / 8),
    SHA1("HmacSHA1", 160 / 8),
}

/**
 * Implementation of the "HMAC-based Extract-and-Expand Key Derivation Function (HKDF)" with
 * support for SHA-1 and SHA-256.
 *
 * See: https://www.rfc-editor.org/rfc/rfc5869
 */
internal class Hkdf(private val alg: HmacAlgorithm) {
    private val mac: Mac = Mac.getInstance(alg.algorithm)

    fun derive(salt: ByteArray?, ikm: ByteArray, info: ByteArray, l: Int): ByteArray {
        val prk = extract(salt = salt, ikm = ikm)
        return expand(prk = prk, info = info, l = l)
    }

    @VisibleForTesting
    internal fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val saltBytes = salt ?: ByteArray(alg.hashLen)
        return hmac(k = saltBytes, data = ikm)
    }

    @VisibleForTesting
    internal fun expand(prk: ByteArray, info: ByteArray, l: Int): ByteArray {
        // this is ceil(l / hashLen)
        val iterations = ceilOfIntegerDivision(l, alg.hashLen)
        check(iterations < 256)

        var t = ByteArray(0)
        val buffer = ByteBuffer.allocate(iterations * alg.hashLen)
        for (i in 1..iterations) {
            t = hmac(k = prk, data = concat(t, info, byteArrayOf(i.toByte())))
            buffer.put(t)
        }
        return buffer.array().sliceArray(0 until l)
    }

    private fun hmac(k: ByteArray, data: ByteArray): ByteArray {
        mac.init(PermissiveSecretKeySpec(k, alg.algorithm))
        return mac.doFinal(data) // also resets the instance
    }
}

/**
 * Custom KeySpec to allow for empty keys as required by the HKDF spec. Note that an "empty key"
 * is not the same as "no key".
 */
private class PermissiveSecretKeySpec(
    private val key: ByteArray,
    private val algorithm: String,
) : KeySpec, SecretKey {
    override fun getAlgorithm() = algorithm
    override fun getFormat() = "RAW"
    override fun getEncoded() = key
}
