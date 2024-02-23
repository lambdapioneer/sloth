package com.lambdapioneer.sloth.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith


@Suppress("UsePropertyAccessSyntax")
@RunWith(AndroidJUnit4::class)
class Pbkdf2PwHashTest {

    private val instance = Pbkdf2PwHash()
    private val salt = instance.createSalt()

    @Test
    fun testHash_whenSameInput_thenSameOutput() {
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)
        val h2 = instance.deriveHash(salt, "test".toCharArray(), 32)

        assertThat(h1).isEqualTo(h2)
    }

    @Test
    fun testHash_whenInteractiveParameters_thenSucceeds() {
        val instance = Pbkdf2PwHash(Pbkdf2PwHashParams.INTERACTIVE)
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)

        assertThat(h1).isNotEmpty()
    }

    @Test
    fun testHash_whenOwaspParameters_thenSucceeds() {
        val instance = Pbkdf2PwHash(Pbkdf2PwHashParams.OWASP)
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)

        assertThat(h1).isNotEmpty()
    }

    @Test
    fun testHash_whenOwaspSha256Parameters_thenSucceeds() {
        val instance = Pbkdf2PwHash(Pbkdf2PwHashParams.OWASP_SHA256)
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)

        assertThat(h1).isNotEmpty()
    }

    @Test
    fun testHash_whenLongKeyLength_thenSucceeds() {
        val len = 10 * 1024 * 1024 // 10 MiB
        val h = instance.deriveHash(salt, "test".toCharArray(), len)

        assertThat(h).hasSize(len)
    }

    @Test
    fun testHash_whenDifferentSalt_thenDifferentOutput() {
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)

        val differentSalt = instance.createSalt()
        val h2 = instance.deriveHash(differentSalt, "test".toCharArray(), 32)

        assertThat(h1).isNotEqualTo(h2)
    }

    @Test
    fun testHash_whenDifferentPassword_thenDifferentOutput() {
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)
        val h2 = instance.deriveHash(salt, "different".toCharArray(), 32)

        assertThat(h1).isNotEqualTo(h2)
    }
}
