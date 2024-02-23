package com.lambdapioneer.sloth.pwhash_libsodium

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class LibSodiumArgon2PwHashTest {

    private val instance = LibSodiumArgon2PwHash()
    private val salt = instance.createSalt()

    @Test
    fun testHash_whenSameInput_thenSameOutput() {
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)
        val h2 = instance.deriveHash(salt, "test".toCharArray(), 32)

        assertThat(h1).isEqualTo(h2)
    }

    @Test
    fun testHash_whenModerateParameters_thenSucceeds() {
        val instance = LibSodiumArgon2PwHash(LibSodiumArgon2PwHashParams.MODERATE)
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)

        assertThat(h1).isNotEmpty()
    }

    @Test
    fun testHash_whenInteractiveParameters_thenSucceeds() {
        val instance = LibSodiumArgon2PwHash(LibSodiumArgon2PwHashParams.INTERACTIVE)
        val h1 = instance.deriveHash(salt, "test".toCharArray(), 32)

        assertThat(h1).isNotEmpty()
    }

    @Test
    fun testHash_whenOwaspParameters_thenSucceeds() {
        val instance = LibSodiumArgon2PwHash(LibSodiumArgon2PwHashParams.OWASP)
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

    /**
     * This method ensures that the returned hash remains backwards-compatible between different
     * releases. Do not update the `expected` output.
     */
    @Test
    fun testHash_whenGivenFixedInput_thenMatchesExpected() {
        internalTestVectorAssertion(
            params = LibSodiumArgon2PwHashParams.INTERACTIVE,
            expected = "4ff03446cb47263568f801f9e36412b4c182b4a039ac84fe3bb03c3528961491"
        )
        internalTestVectorAssertion(
            params = LibSodiumArgon2PwHashParams.MODERATE,
            expected = "43de47cf7103d0fa6aa9dd1425089fcd4c793fc6c75496089396814517f97ce8"
        )
        internalTestVectorAssertion(
            params = LibSodiumArgon2PwHashParams.OWASP,
            expected = "9df7459c90d889cf7e255e517c4299392ce860eed2d20f1a7cecda42bb2998bc"
        )
    }

    private fun internalTestVectorAssertion(params: LibSodiumArgon2PwHashParams, expected: String) {
        val salt = "00112233445566778899AABBCCDDEEFF".decodeAsHex()
        val password = "passphrase äüö"

        val instance = LibSodiumArgon2PwHash(params)
        val actual = instance.deriveHash(salt, password.toCharArray(), 32)
        assertThat(actual).isEqualTo(expected.decodeAsHex())
    }
}

private fun String.decodeAsHex() = ByteArray(this.length / 2) {
    this.substring(2 * it, 2 * it + 2).toInt(radix = 16).toByte()
}
