package com.lambdapioneer.sloth.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.crypto.Pbkdf2PwHash
import com.lambdapioneer.sloth.storage.OnDiskStorage
import com.lambdapioneer.sloth.testing.createSecureElementOrSkip
import com.lambdapioneer.sloth.utils.secureRandomBytes
import com.lambdapioneer.sloth.utils.secureRandomChars
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private val DEFAULT_HANDLE = "handle".encodeToByteArray()
private val DEFAULT_PASSWORD = "password".toCharArray()
private const val DEFAULT_OUTPUT_LENGTH = 32

@RunWith(AndroidJUnit4::class)
class LongSlothImplTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val baseStorage = OnDiskStorage(context)
    private val storage = baseStorage.getOrCreateNamespace("LongSlothTest")

    @Before
    fun setUp() {
        baseStorage.basePath().deleteRecursively()
    }

    @Test
    fun whenKeyGenAndDerive_thenYieldSameKey() {
        val instance = createInstance()

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
    fun whenKeyGenWithLongPassword_thenYieldSameKey() {
        val instance = createInstance()
        val pw = secureRandomChars(255)

        val k1 = instance.keyGen(
            storage = storage,
            pw = pw,
            h = DEFAULT_HANDLE,
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        val k2 = instance.derive(
            storage = storage,
            pw = pw,
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        assertThat(k1).isEqualTo(k2)
    }

    @Test
    fun whenKeyGenAndDeriveWithDifferentPassword_thenYieldDifferentKeys() {
        val instance = createInstance()

        val k1 = instance.keyGen(
            storage = storage,
            pw = DEFAULT_PASSWORD,
            h = DEFAULT_HANDLE,
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        val k2 = instance.derive(
            storage = storage,
            pw = "not the default password".toCharArray(),
            outputLengthBytes = DEFAULT_OUTPUT_LENGTH
        )

        assertThat(k1).isNotEqualTo(k2)
    }

    // all files that are expected to be created by a first call of LongSloth on an empty storage
    private val allFiles = listOf(
        File(context.filesDir, "sloth"),
        File(context.filesDir, "sloth/LongSlothTest"),
        File(context.filesDir, "sloth/LongSlothTest/h"),
        File(context.filesDir, "sloth/LongSlothTest/salt"),
    )

    @Test
    fun whenCallingOnAppStart_thenAllFilesCreated() {
        val instance = createInstance()
        instance.onAppStart(storage, DEFAULT_HANDLE)

        val actualFiles = baseStorage.basePath().walkTopDown().toList()
        assertThat(actualFiles).containsExactlyInAnyOrderElementsOf(allFiles)
    }

    // all files that are expected to have their timestamp updated by a second call of LongSloth
    private val criticalFiles = listOf(
        File(context.filesDir, "sloth/LongSlothTest"),
        File(context.filesDir, "sloth/LongSlothTest/h"),
        File(context.filesDir, "sloth/LongSlothTest/salt"),
    )

    @Test
    fun whenCallingOnAppStartTwice_thenTimestampsOfAllCriticalFilesAreUpdated() {
        val instance = createInstance()
        instance.onAppStart(storage, DEFAULT_HANDLE)

        val timeStampsBefore = criticalFiles.map { it.lastModified() }

        Thread.sleep(1001)
        instance.onAppStart(storage, DEFAULT_HANDLE)

        val timeStampsAfter = criticalFiles.map { it.lastModified() }

        assertThat(timeStampsBefore.size).isEqualTo(timeStampsAfter.size)
        timeStampsBefore.zip(timeStampsAfter).forEach { (before, after) ->
            assertThat(after).isGreaterThan(before)
        }
    }

    private fun createInstance() = LongSlothImpl(
        params = LongSlothParams(),
        secureElement = createSecureElementOrSkip(context),
        pwHash = Pbkdf2PwHash()
    )
}
