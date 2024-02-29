package com.lambdapioneer.sloth.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.CharBuffer
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.nextInt

@RunWith(AndroidJUnit4::class)
class SecureRandomExtensionsTests {

    @Test
    fun testSecureRandomBytes_whenCalledMultipleTimes_thenDifferentResults() {
        val arr1 = secureRandomBytes(length = 32)
        val arr2 = secureRandomBytes(length = 32)
        assertThat(arr1).isNotEqualTo(arr2)
    }

    @Test
    fun testSecureRandomChars_whenCalledMultipleTimes_thenDifferentResults() {
        val arr1 = secureRandomChars(length = 32)
        val arr2 = secureRandomChars(length = 32)
        assertThat(arr1).isNotEqualTo(arr2)
    }

    @Test
    fun testSecureRandomChars_whenCalledWithEntropy_thenResultingLengthsPlausible() {
        // for a value lower than the per-character entropy, the resulting array should one
        // character long
        val entropyLow = 10.0
        val arrLow = secureRandomChars(entropy = entropyLow)
        assertThat(arrLow).hasSize(1)

        // for a value just above the per-character entropy, the resulting array should be two
        // characters long
        val entropyTwo = 16.0
        val arrTwo = secureRandomChars(entropy = entropyTwo)
        assertThat(arrTwo).hasSize(2)

        // for a practical (high) value, the length should match our expectations and be rounded up
        val entropyHigh = 128.0
        val arrHigh = secureRandomChars(entropy = entropyHigh)
        assertThat(arrHigh).hasSize(128 / 16 + 1)
    }

    @Test
    fun testSecureRandomChars_whenGeneratingMany_thenAllAreValidStrings() {
        val n = 10_000
        val r = Random(0)
        repeat(n) {
            val length = r.nextInt(1..64)
            val arr = secureRandomChars(length = length)

            // ensure that encoding directly works
            val encodedByteBufferDirectly = Charsets.UTF_8.encode(CharBuffer.wrap(arr))
            val encodedByteArrayDirectly = encodedByteBufferDirectly.array().copyOfRange(
                fromIndex = 0,
                toIndex = encodedByteBufferDirectly.limit()
            )
            assertThat(encodedByteArrayDirectly).isNotEmpty()
            assertThat(encodedByteArrayDirectly.size).isGreaterThanOrEqualTo(length)

            // ensure that encoding via String works
            val encodedByteArrayViaString = String(arr).encodeToByteArray()
            assertThat(encodedByteArrayViaString).isNotEmpty()
            assertThat(encodedByteArrayViaString.size).isGreaterThanOrEqualTo(length)

            // also, both should be equal
            assertThat(encodedByteArrayDirectly).isEqualTo(encodedByteArrayViaString)
        }
    }

    @Test
    @Suppress("DEPRECATION") // for `Char.toInt()`
    fun testSecureRandomNextChar_whenGeneratingMany_thenPlausiblyDistributedAmongValidChars() {
        val validRangePreSurrogates = 0x0000 until 0xD800 // start inclusive, end exclusive
        val invalidRangeSurrogates = 0xD800..0xDFFF // start inclusive, end inclusive
        val validRangePostSurrogates = 0xE000..0xFFFF // start inclusive, end inclusive

        // ensure that our ranges have same cardinality as all possible `Char` values
        val numValidCharValues = validRangePreSurrogates.count() + validRangePostSurrogates.count()
        val numTotalCharValues = numValidCharValues + invalidRangeSurrogates.count()
        assertThat(numTotalCharValues).isEqualTo(Char.MAX_VALUE.toInt() + 1) // fence post

        // generate an average of 10 hits per valid characters
        val target = 10.0
        val n = (numValidCharValues * target).toInt()
        val counts = IntArray(size = numTotalCharValues)
        val sr = SecureRandom()
        repeat(n) {
            val c = sr.nextChar()
            counts[c.toInt()]++
        }

        fun getAverageHitsForRange(range: IntRange): Double {
            var sum = 0.0
            range.forEach { sum += counts[it] }
            return sum / range.count()
        }

        // we expect an average of 10 hits in the valid ranges, and none in the "invalid" range
        val offset = Offset.offset(0.1)
        assertThat(getAverageHitsForRange(validRangePreSurrogates)).isCloseTo(target, offset)
        assertThat(getAverageHitsForRange(invalidRangeSurrogates)).isEqualTo(0.0)
        assertThat(getAverageHitsForRange(validRangePostSurrogates)).isCloseTo(target, offset)
    }
}
