package com.lambdapioneer.sloth.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.crypto.Pbkdf2PwHash
import com.lambdapioneer.sloth.storage.OnDiskStorage
import com.lambdapioneer.sloth.testing.KIB
import com.lambdapioneer.sloth.testing.MIB
import com.lambdapioneer.sloth.utils.NoopTracer
import com.lambdapioneer.sloth.utils.secureRandomBytes
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.AEADBadTagException

private val DEFAULT_HANDLE = "handle".encodeToByteArray()
private const val DEFAULT_PASSWORD = "password"

@RunWith(AndroidJUnit4::class)
class HiddenSlothTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = OnDiskStorage(context)

    @Test
    fun testHiddenSloth_whenInit_thenNoThrows() {
        val instance = createInstance()
        instance.init(storage, DEFAULT_HANDLE)
    }

    @Test
    fun testHiddenSloth_whenEncrypt_thenDecryptReturnsSame() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)
        val actual = instance.decrypt(storage, DEFAULT_PASSWORD)

        assertThat(actual).isEqualTo(data)
    }

    @Test
    fun testHiddenSloth_whenEncryptMaxSize_thenDecryptReturnsSame() {
        val instance = createInstance()
        val data = secureRandomBytes(instance.maxPayloadSize())

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)
        val actual = instance.decrypt(storage, DEFAULT_PASSWORD)

        assertThat(actual).isEqualTo(data)
    }

    @Test
    fun testHiddenSloth_whenEncrypt100MiB_thenDecryptReturnsSame() {
        val instance = createInstance(size = 100 * MIB)
        val data = secureRandomBytes(instance.maxPayloadSize())

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)
        val actual = instance.decrypt(storage, DEFAULT_PASSWORD)

        assertThat(actual).isEqualTo(data)
    }

    @Test
    fun testHiddenSloth_whenDecryptTwice_thenReturnsSame() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)

        val actual1 = instance.decrypt(storage, DEFAULT_PASSWORD)
        assertThat(actual1).isEqualTo(data)

        val actual2 = instance.decrypt(storage, DEFAULT_PASSWORD)
        assertThat(actual2).isEqualTo(data)
    }

    @Test(expected = AEADBadTagException::class)
    fun testHiddenSloth_whenEncrypt_thenDecryptWithWrongPasswordThrows() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)
        instance.decrypt(storage, "wrong passphrase")
    }

    @Test(expected = AEADBadTagException::class)
    fun testHiddenSloth_whenEncrypt_thenInitAndThenDecryptThrows() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)
        instance.init(storage, DEFAULT_HANDLE)
        instance.decrypt(storage, DEFAULT_PASSWORD)
    }

    @Test
    fun testHiddenSloth_whenEncryptAndRatchet_thenDecryptReturnsSame() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)
        instance.ratchet(storage, NoopTracer())
        val actual = instance.decrypt(storage, DEFAULT_PASSWORD)

        assertThat(actual).isEqualTo(data)
    }

    private fun createInstance(size: Int = 100 * KIB) = HiddenSlothImpl(
        params = HiddenSlothParams(storageTotalSize = size),
        secureElement = com.lambdapioneer.sloth.testing.createSecureElementOrSkip(
            context
        ),
        pwHash = Pbkdf2PwHash(),
    )
}
