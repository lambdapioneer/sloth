package com.lambdapioneer.sloth.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilsTest {

    @Test
    fun testHexEncodeDecode_whenEmpty_thenEmpty() {
        val bs = byteArrayOf()
        assertThat(bs.encodeAsHex()).hasSize(0)

        val actual = bs.encodeAsHex().decodeAsHex()
        assertThat(bs).isEqualTo(actual)
    }

    @Test
    fun testHexEncodeDecode_whenWildBytesString_thenMatches() {
        val bs = secureRandomBytes(1024)
        assertThat(bs.encodeAsHex()).hasSize(1024 * 2)

        val actual = bs.encodeAsHex().decodeAsHex()
        assertThat(bs).isEqualTo(actual)
    }
}
