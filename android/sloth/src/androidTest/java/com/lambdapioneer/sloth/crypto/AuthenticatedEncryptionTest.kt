package com.lambdapioneer.sloth.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lambdapioneer.sloth.testing.MIB
import com.lambdapioneer.sloth.utils.secureRandomBytes
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.AEADBadTagException
import kotlin.experimental.xor

@RunWith(AndroidJUnit4::class)
class AuthenticatedEncryptionTest {

    private val defaultKey = AuthenticatedEncryption.keyGen()
    private val defaultIv = AuthenticatedEncryption.ivGen()

    private val otherKey = AuthenticatedEncryption.keyGen()
    private val otherIv = AuthenticatedEncryption.ivGen()

    @Test
    fun test_whenEncryptEmpty_thenDecryptMatches() {
        val data = "".toByteArray()
        val c = AuthenticatedEncryption.encrypt(defaultKey, defaultIv, data)
        val p = AuthenticatedEncryption.decrypt(defaultKey, defaultIv, c)
        assertThat(p).isEqualTo(data)
    }

    @Test
    fun test_whenEncryptMessage_thenDecryptMatches() {
        val data = "some message".toByteArray()
        val c = AuthenticatedEncryption.encrypt(defaultKey, defaultIv, data)
        val p = AuthenticatedEncryption.decrypt(defaultKey, defaultIv, c)
        assertThat(p).isEqualTo(data)
    }

    @Test
    fun test_whenEncryptMessage_thenUnauthenticatedDecryptMatches() {
        val data = "some message".toByteArray()
        val c = AuthenticatedEncryption.encrypt(defaultKey, defaultIv, data)
        val p = AuthenticatedEncryption.decryptUnauthenticated(
            k = defaultKey,
            iv = defaultIv,
            dataAndTag = c,
            offset = 0,
            length = data.size
        )
        assertThat(p).isEqualTo(data)
    }

    @Test
    fun test_whenEncryptMessage_thenUnauthenticatedDecryptAtOffsetMatches() {
        val data = "abcdef0123456789THIS_IS_A_MESSAGE_123".toByteArray()
        val c = AuthenticatedEncryption.encrypt(defaultKey, defaultIv, data)
        val p = AuthenticatedEncryption.decryptUnauthenticated(
            k = defaultKey,
            iv = defaultIv,
            dataAndTag = c,
            offset = 16, // must be block-aligned
            length = 17
        )
        assertThat(p).isEqualTo("THIS_IS_A_MESSAGE".toByteArray())
    }

    @Test
    fun test_whenReEncryptShortMessage_thenDecryptMatches() {
        val data = "some message".toByteArray()
        val c = AuthenticatedEncryption.encrypt(defaultKey, defaultIv, data)

        AuthenticatedEncryption.inplaceDecryptEncrypt(
            dataAndTag = c,
            decryptK = defaultKey,
            decryptIv = defaultIv,
            encryptK = otherKey,
            encryptIv = otherIv,
        )

        val p = AuthenticatedEncryption.decrypt(otherKey, otherIv, c)
        assertThat(p).isEqualTo(data)
    }

    @Test
    fun test_whenReEncryptLongMessage_thenDecryptMatches() {
        val data = secureRandomBytes(1 * MIB)
        val c = AuthenticatedEncryption.encrypt(defaultKey, defaultIv, data)

        AuthenticatedEncryption.inplaceDecryptEncrypt(
            dataAndTag = c,
            decryptK = defaultKey,
            decryptIv = defaultIv,
            encryptK = otherKey,
            encryptIv = otherIv,
        )

        val p = AuthenticatedEncryption.decrypt(otherKey, otherIv, c)
        assertThat(p).isEqualTo(data)
    }

    @Test(expected = AEADBadTagException::class)
    fun test_whenFlipBit_thenDecryptThrows() {
        val data = "some message".toByteArray()

        val c = AuthenticatedEncryption.encrypt(defaultKey, defaultIv, data)
        c[0] = c[0] xor 0x01

        val p = AuthenticatedEncryption.decrypt(defaultKey, defaultIv, c)
        assertThat(p).isEqualTo(data)
    }
}
