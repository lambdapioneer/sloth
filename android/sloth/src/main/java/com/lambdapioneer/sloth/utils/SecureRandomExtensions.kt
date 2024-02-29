package com.lambdapioneer.sloth.utils

import java.security.SecureRandom
import kotlin.math.ceil


/**
 * Creates a new byte array of random bytes sourced from [SecureRandom].
 */
fun secureRandomBytes(length: Int): ByteArray {
    require(length >= 0)
    val secureRandom = SecureRandom()

    val result = ByteArray(length)
    secureRandom.nextBytes(result)
    return result
}

/**
 * Creates a new array of random Char elements sourced from [SecureRandom.nextChar] of the
 * given [length].
 */
fun secureRandomChars(length: Int): CharArray {
    require(length >= 0)
    val secureRandom = SecureRandom()

    return CharArray(length) { secureRandom.nextChar() }
}

/**
 * Creates a new array of random Char elements sourced from [SecureRandom.nextChar] of a given
 * total entropy assuming 15.954 bits per character.
 */
fun secureRandomChars(entropy: Double): CharArray {
    require(entropy > 0)

    val bitsPerChar = 15.594 // log2(2^16 - 2048) = 15.954 + eps
    val length = ceil(entropy / bitsPerChar).toInt()

    return secureRandomChars(length = length)
}

/**
 * Returns a [Char] element chosen uniformly at random from all single code units excluding the
 * surrogate range (0xD800..0xDFFF). This ensures that an array of results from this function
 * always represents a valid string.
 *
 * Since we exclude a range of 2048 (0x800) characters, the total entropy per character is just
 * log2(2^16 - 2^11) ~= 15.954...
 */
@Suppress("DEPRECATION") // for `Char.toInt()`
fun SecureRandom.nextChar(): Char {
    val surrogateRangeSize = Char.MAX_SURROGATE.toInt() - Char.MIN_SURROGATE.toInt() + 1
    val i = nextInt(Char.MAX_VALUE.toInt() - surrogateRangeSize)

    return if (i >= Char.MIN_SURROGATE.toInt()) {
        (i + surrogateRangeSize).toChar()
    } else {
        i.toChar()
    }
}
