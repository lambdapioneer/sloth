package com.lambdapioneer.sloth.crypto

import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.utils.ceilOfIntegerDivision
import com.lambdapioneer.sloth.utils.concat
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


private const val HMAC_SHA256 = "HmacSHA256"
private const val HMAC_SHA256_LEN = 256 / 8

/**
 * Implementation of the " HMAC-based Extract-and-Expand Key Derivation Function (HKDF)" with
 * support for only SHA-256.
 *
 * See: https://www.rfc-editor.org/rfc/rfc5869
 */
internal class Hkdf {

    fun derive(salt: ByteArray, ikm: ByteArray, info: ByteArray, l: Int): ByteArray {
        val prk = extract(salt = salt, ikm = ikm)
        return expand(prk = prk, info = info, l = l)
    }

    @VisibleForTesting
    internal fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val saltBytes = salt ?: ByteArray(HMAC_SHA256_LEN)
        return hmacSha256(saltBytes, ikm)
    }

    @VisibleForTesting
    internal fun expand(prk: ByteArray, info: ByteArray, l: Int): ByteArray {
        // this is ceil(l / HMAC_SHA256_LEN)
        val iterations = ceilOfIntegerDivision(l, HMAC_SHA256_LEN)
        check(iterations < 256)

        var t = ByteArray(0)
        val buffer = ByteBuffer.allocate(iterations * HMAC_SHA256_LEN)
        for (i in 1..iterations) {
            t = hmacSha256(prk, concat(t, info, byteArrayOf(i.toByte())))
            buffer.put(t)
        }
        return buffer.array().sliceArray(0 until l)
    }

    private fun hmacSha256(k: ByteArray, data: ByteArray): ByteArray {
        // TODO: re-use instance in expand iterations
        val mac: Mac = Mac.getInstance(HMAC_SHA256)

        // TODO: use custom key spec that allows for an empty key
        mac.init(SecretKeySpec(k, HMAC_SHA256))

        return mac.doFinal(data)
    }
}
