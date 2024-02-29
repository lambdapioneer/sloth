package com.lambdapioneer.sloth.utils

import java.nio.ByteBuffer


/**
 * Decodes the given hex string as a byte array.
 */
internal fun String.decodeAsHex(): ByteArray {
    require(this.length % 2 == 0)

    return ByteArray(this.length / 2) {
        this.substring(2 * it, 2 * it + 2).toInt(radix = 16).toByte()
    }
}

/**
 * Encodes the given byte array as a lower-case hex string.
 */
internal fun ByteArray.encodeAsHex(): String {
    return StringBuilder(size * 2).run {
        for (b in this@encodeAsHex) append(String.format("%02x", b))
        toString()
    }
}

/**
 * Concatenates the given byte arrays.
 */
internal fun concat(vararg xs: ByteArray): ByteArray {
    val buffer = ByteBuffer.allocate(xs.sumOf { it.size })
    for (x in xs) {
        buffer.put(x)
    }
    return buffer.array()
}
