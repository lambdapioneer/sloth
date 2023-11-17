package com.lambdapioneer.sloth.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.crypto.Pbkdf2PwHash
import com.lambdapioneer.sloth.storage.InMemoryStorage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private val DEFAULT_HANDLE = "handle".encodeToByteArray()
private const val DEFAULT_PASSWORD = "password"
private const val DEFAULT_OUTPUT_LENGTH = 32

@RunWith(AndroidJUnit4::class)
class LongSlothTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun whenKeyGenAndDerive_thenYieldSameKey() {
        val instance = createInstance()
        val storage = InMemoryStorage()

        val k1 = instance.keyGen(
            storage = storage,
            pw = DEFAULT_PASSWORD,
            h = DEFAULT_HANDLE,
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        val k2 = instance.derive(
            storage = storage,
            pw = DEFAULT_PASSWORD,
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        assertThat(k1).isEqualTo(k2)
    }

    @Test
    fun whenKeyGenAndDeriveWithDifferentPassword_thenYieldDifferentKeys() {
        val instance = createInstance()
        val storage = InMemoryStorage()

        val k1 = instance.keyGen(
            storage = storage,
            pw = DEFAULT_PASSWORD,
            h = DEFAULT_HANDLE,
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        val k2 = instance.derive(
            storage = storage,
            pw = "not the default password",
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        assertThat(k1).isNotEqualTo(k2)
    }

    private fun createInstance() = LongSlothImpl(
        params = LongSlothParams(),
        secureElement = com.lambdapioneer.sloth.testing.createSecureElementOrSkip(
            context
        ),
        pwHash = Pbkdf2PwHash()
    )
}
