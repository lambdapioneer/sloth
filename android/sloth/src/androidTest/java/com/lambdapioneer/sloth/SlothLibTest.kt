package com.lambdapioneer.sloth;

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.impl.HiddenSlothParams
import com.lambdapioneer.sloth.impl.LongSlothParams
import com.lambdapioneer.sloth.storage.OnDiskStorage
import com.lambdapioneer.sloth.testing.MIB
import com.lambdapioneer.sloth.testing.createSecureElementOrSkip
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SlothLibTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testSlothLib_whenInit_thenNoThrows() {
        val instance = createInstance()
        instance.init(context)
    }

    @Test(expected = IllegalStateException::class)
    fun testSlothLib_whenDoubleInit_thenThrows() {
        val instance = createInstance()
        instance.init(context)
        instance.init(context)
    }

    @Test
    fun testLongSloth_runThrough() {
        val instance = createInstance()

        val pw = "password"
        val storage = OnDiskStorage(context)
        val identifier1 = "longsloth_test_1"
        val longSloth1 = instance.getLongSlothInstance(
            identifier = identifier1,
            storage = storage,
            params = LongSlothParams()
        )

        // create a key and verify that re-deriving results in the same key
        val key1a = longSloth1.createNewKey(pw)
        val key1b = longSloth1.deriveForExistingKey(pw)
        assertThat(key1a).isEqualTo(key1b)

        // create a key under a different namespace and verify it is different
        val identifier2 = "longsloth_test_2"
        val longSloth2 = instance.getLongSlothInstance(
            identifier = identifier2,
            storage = storage,
            params = LongSlothParams()
        )

        val key2 = longSloth2.createNewKey(pw)
        assertThat(key1a).isNotEqualTo(key2)

        // verify the original key still derives correctly
        val key1c = longSloth1.deriveForExistingKey(pw)
        assertThat(key1a).isEqualTo(key1c)

        // reset the key and verify that this results in a different key
        val key1d = longSloth1.createNewKey(pw)
        assertThat(key1d).isNotEqualTo(key1a)

        // check that the key exists
        assertThat(longSloth1.hasKey()).isEqualTo(true)

        // remove
        longSloth1.deleteKey()
        assertThat(longSloth1.hasKey()).isEqualTo(false)

        // verify that re-deriving fails
        try {
            longSloth1.deriveForExistingKey(pw)
            throw AssertionError("Expected IllegalStateException")
        } catch (e: SlothStorageKeyNotFound) {
            // expected
        }
    }

    @Test
    fun testHiddenSloth_runThrough() {
        val instance = createInstance()
        val storage = OnDiskStorage(context)

        val data = "hello".toByteArray()
        val pw = "password"

        // create a HiddenSloth instance
        val params = HiddenSlothParams(storageTotalSize = 1 * MIB)
        val hiddenSloth1 = instance.getHiddenSlothInstance(
            identifier = "hidden_sloth_test",
            storage = storage,
            params = params
        )

        // clear existing storage (if it exists)
        hiddenSloth1.removeStorage()

        // initialize a storage
        hiddenSloth1.ensureStorage()

        // ensure we can see it from another instance
        val hiddenSloth2 = instance.getHiddenSlothInstance(
            identifier = "hidden_sloth_test",
            storage = storage,
            params = params
        )
        hiddenSloth2.hasStorage()

        // encrypt data under passphrase
        hiddenSloth1.encryptToStorage(pw, data)

        // ratchet once
        hiddenSloth1.ratchet()

        // decrypts under the same passphrase
        val actual = hiddenSloth1.decryptFromStorage(pw)
        assertThat(actual).isEqualTo(data)

        // after "re-ensuring" the storage, we can still decrypt
        hiddenSloth1.ensureStorage()
        val actual2 = hiddenSloth1.decryptFromStorage(pw)
        assertThat(actual2).isEqualTo(data)

        // does not decrypt under a different passphrase
        assertThatExceptionOfType(SlothDecryptionFailed::class.java).isThrownBy {
            hiddenSloth1.decryptFromStorage("wrong passphrase")
        }

        // decrypts from another instance
        val hiddenSloth3 = instance.getHiddenSlothInstance(
            identifier = "hidden_sloth_test",
            storage = storage,
            params = params
        )
        hiddenSloth3.ensureStorage()
        val actual3 = hiddenSloth3.decryptFromStorage(pw)
        assertThat(actual3).isEqualTo(data)
    }

    private fun createInstance(): SlothLib {
        val instance = SlothLib()

        val secureElement = createSecureElementOrSkip(context = context)
        instance.setSecureElementForTesting(secureElement = secureElement)

        return instance
    }
}
