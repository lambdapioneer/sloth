package com.lambdapioneer.sloth.secureelement

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private val DEFAULT_HANDLE = KeyHandle("test")
private val DEFAULT_IV = "0123456789ABCDEF".encodeToByteArray() // 16 bytes

@RunWith(AndroidJUnit4::class)
class SecureElementTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun whenDecryptWithNewGenKey_thenSameResultAfterReLoadingKey() {
        val instance =
            com.lambdapioneer.sloth.testing.createSecureElementOrSkip(context)
        instance.aesCtrGenKey(keyHandle = DEFAULT_HANDLE)

        val data = "hello world".encodeToByteArray()
        val c1 = instance.aesCtrDecrypt(
            keyHandle = DEFAULT_HANDLE,
            iv = DEFAULT_IV,
            data = data
        )

        val c2 = instance.aesCtrDecrypt(
            keyHandle = DEFAULT_HANDLE,
            iv = DEFAULT_IV,
            data = data
        )

        assertThat(c1).isEqualTo(c2)
    }

    @Test
    fun whenDecryptWithDifferentIV_thenDifferentResult() {
        val instance =
            com.lambdapioneer.sloth.testing.createSecureElementOrSkip(context)
        instance.aesCtrGenKey(keyHandle = DEFAULT_HANDLE)

        val data = "hello world".encodeToByteArray()
        val c1 = instance.aesCtrDecrypt(
            keyHandle = DEFAULT_HANDLE,
            iv = DEFAULT_IV,
            data = data
        )

        val otherIv = instance.aesCtrGenIv()

        val c2 = instance.aesCtrDecrypt(
            keyHandle = DEFAULT_HANDLE,
            iv = otherIv,
            data = data
        )

        assertThat(c1).isNotEqualTo(c2)
    }

    @Test
    fun whenDecryptWithKeyResetInBetween_thenDifferentResult() {
        val instance =
            com.lambdapioneer.sloth.testing.createSecureElementOrSkip(context)
        instance.aesCtrGenKey(keyHandle = DEFAULT_HANDLE)

        val data = "hello world".encodeToByteArray()
        val c1 = instance.aesCtrDecrypt(
            keyHandle = DEFAULT_HANDLE,
            iv = DEFAULT_IV,
            data = data
        )

        instance.aesCtrGenKey(keyHandle = DEFAULT_HANDLE)

        val c2 = instance.aesCtrDecrypt(
            keyHandle = DEFAULT_HANDLE,
            iv = DEFAULT_IV,
            data = data
        )

        assertThat(c1).isNotEqualTo(c2)
    }

    @Test
    fun whenEncrypt_thenDecryptReturnsSame() {
        val instance =
            com.lambdapioneer.sloth.testing.createSecureElementOrSkip(context)
        val data = "hello world".encodeToByteArray()

        instance.aesCtrGenKey(keyHandle = DEFAULT_HANDLE)
        val c = instance.aesCtrEncrypt(DEFAULT_HANDLE, DEFAULT_IV, data)
        val p = instance.aesCtrDecrypt(DEFAULT_HANDLE, DEFAULT_IV, c)

        assertThat(p).isEqualTo(data)

    }
}
