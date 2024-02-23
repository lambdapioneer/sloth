package com.lambdapioneer.sloth.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.crypto.Pbkdf2PwHash
import com.lambdapioneer.sloth.storage.OnDiskStorage
import com.lambdapioneer.sloth.testing.KIB
import com.lambdapioneer.sloth.testing.MIB
import com.lambdapioneer.sloth.testing.createSecureElementOrSkip
import com.lambdapioneer.sloth.utils.NoopTracer
import com.lambdapioneer.sloth.utils.secureRandomBytes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.crypto.AEADBadTagException

private val DEFAULT_HANDLE = "handle".encodeToByteArray()
private val DEFAULT_PASSWORD = "password".toCharArray()

@RunWith(AndroidJUnit4::class)
class HiddenSlothImplTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val baseStorage = OnDiskStorage(context)
    private val storage = baseStorage.getOrCreateNamespace("HiddenSlothTest")
    private val defaultPayloadLength = 10 * KIB

    @Before
    fun setUp() {
        baseStorage.basePath().deleteRecursively()
    }

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
        val data = secureRandomBytes(defaultPayloadLength)

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)

        val actual = instance.decrypt(storage, DEFAULT_PASSWORD)
        assertThat(actual).isEqualTo(data)
    }

    @Test
    fun testHiddenSloth_whenEncryptMaxSize_thenOnDiskCiphertextHasExpectedSize() {
        val instance = createInstance(payloadLength = defaultPayloadLength)
        val data = secureRandomBytes(defaultPayloadLength)

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)

        val blob = storage.get(HiddenSlothKeys.BLOB.key)
        val expectedSize = defaultPayloadLength + HiddenSlothImpl.ciphertextTotalOverhead()
        assertThat(blob.size).isEqualTo(expectedSize)
    }

    @Test
    fun testHiddenSloth_whenEncryptMoreThanMaxSize_thenFails() {
        val instance = createInstance()
        val data = secureRandomBytes(defaultPayloadLength + 1)

        instance.init(storage, DEFAULT_HANDLE)
        assertThatThrownBy { instance.encrypt(storage, DEFAULT_PASSWORD, data) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("payload too large")
    }

    @Test
    fun testHiddenSloth_whenEncrypt100MiB_thenDecryptReturnsSame() {
        val instance = createInstance(payloadLength = 100 * MIB)
        val data = secureRandomBytes(100 * MIB)

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
        instance.decrypt(storage, "wrong passphrase".toCharArray())
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

    @Test
    fun testHiddenSloth_whenEncryptNormalAndDecryptCached_thenResultMatches() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)
        instance.encrypt(storage, DEFAULT_PASSWORD, data)

        val cachedSecrets = instance.computeCachedSecrets(storage, DEFAULT_PASSWORD)
        val actual = instance.decrypt(storage, pw=null, cachedSecrets=cachedSecrets)
        assertThat(actual).isEqualTo(data)
    }

    @Test
    fun testHiddenSloth_whenEncryptCachedAndDecryptNormal_thenResultMatches() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)

        val cachedSecrets = instance.computeCachedSecrets(storage, DEFAULT_PASSWORD)
        instance.encrypt(storage, pw=null, data=data, cachedSecrets=cachedSecrets)

        val actual = instance.decrypt(storage, DEFAULT_PASSWORD)
        assertThat(actual).isEqualTo(data)
    }

    @Test
    fun testHiddenSloth_whenEncryptAndDecryptWithSameCached_thenResultMatches() {
        val instance = createInstance()
        val data = "hello".toByteArray()

        instance.init(storage, DEFAULT_HANDLE)
        val cachedSecrets = instance.computeCachedSecrets(storage, DEFAULT_PASSWORD)

        // initial encryption with "fresh" secrets
        instance.encrypt(storage, pw=null, data=data, cachedSecrets=cachedSecrets)

        val actual1 = instance.decrypt(storage, DEFAULT_PASSWORD)
        assertThat(actual1).isEqualTo(data)

        // encrypt without secret
        instance.encrypt(storage, pw= DEFAULT_PASSWORD, data=data)

        val actual2 = instance.decrypt(storage, DEFAULT_PASSWORD)
        assertThat(actual2).isEqualTo(data)

        // encrypt with "old" secrets
        instance.encrypt(storage, pw=null, data=data, cachedSecrets=cachedSecrets)

        val actual3 = instance.decrypt(storage, DEFAULT_PASSWORD)
        assertThat(actual3).isEqualTo(data)
    }

    // all files that are expected to be created by a first call of HiddenSloth on an empty storage
    private val allFiles = listOf(
        File(context.filesDir, "sloth"),
        // from LongSloth
        File(context.filesDir, "sloth/HiddenSlothTest"),
        File(context.filesDir, "sloth/HiddenSlothTest/h"),
        File(context.filesDir, "sloth/HiddenSlothTest/salt"),
        // from HiddenSloth
        File(context.filesDir, "sloth/HiddenSlothTest/outer_h"),
        File(context.filesDir, "sloth/HiddenSlothTest/outer_iv"),
        File(context.filesDir, "sloth/HiddenSlothTest/tk"),
        File(context.filesDir, "sloth/HiddenSlothTest/tiv"),
        File(context.filesDir, "sloth/HiddenSlothTest/iv"),
        File(context.filesDir, "sloth/HiddenSlothTest/blob"),
    )

    @Test
    fun testHiddenSloth_whenCallingOnAppStart_thenExactlyAllFilesCreated() {
        val instance = createInstance()
        instance.onAppStart(storage, DEFAULT_HANDLE)

        val actualFiles = baseStorage.basePath().walkTopDown().toList()
        assertThat(actualFiles).containsExactlyInAnyOrderElementsOf(allFiles)
    }

    @Test
    fun testHiddenSloth_whenCallingAllWriteMethods_thenExactlyAllFilesCreated() {
        val instance = createInstance()

        instance.onAppStart(storage, DEFAULT_HANDLE)
        instance.ratchet(storage)
        instance.encrypt(storage, DEFAULT_PASSWORD, "hello".toByteArray())

        val actualFiles = baseStorage.basePath().walkTopDown().toList()
        assertThat(actualFiles).containsExactlyInAnyOrderElementsOf(allFiles)
    }

    @Test
    fun testHiddenSloth_whenCallingOnAppStart_thenAllFileTimestampsAreUpdatedExceptSlothRoot() {
        val instance = createInstance()
        instance.onAppStart(storage, DEFAULT_HANDLE)

        val allFilesExceptRoot = allFiles.filter { it != File(context.filesDir, "sloth") }
        assertThat(allFilesExceptRoot.size).isEqualTo(allFiles.size - 1)

        val timeStampsBefore = allFilesExceptRoot.map { it.lastModified() }

        Thread.sleep(1001)
        instance.onAppStart(storage, DEFAULT_HANDLE)

        val timeStampsAfter = allFilesExceptRoot.map { it.lastModified() }

        assertThat(timeStampsBefore.size).isEqualTo(timeStampsAfter.size)
        timeStampsBefore.zip(timeStampsAfter).forEach { (before, after) ->
            assertThat(after).isGreaterThan(before)
        }
    }

    // All files that are expected to change in content when encrypt or ratchet is called. In short
    // we expect everything other than the key handles (and the LongSloth salt) to change.
    //
    // Note: it is less important which files are changed, but rather that the set is the same for
    // encrypt and ratchet
    private val changingFiles = listOf(
        File(context.filesDir, "sloth/HiddenSlothTest/outer_iv"),
        File(context.filesDir, "sloth/HiddenSlothTest/tk"),
        File(context.filesDir, "sloth/HiddenSlothTest/tiv"),
        File(context.filesDir, "sloth/HiddenSlothTest/iv"),
        File(context.filesDir, "sloth/HiddenSlothTest/blob"),
    )

    @Test
    fun testHiddenSloth_whenEncrypting_thenExactlyTheCriticalFilesAreUpdated() {
        val instance = createInstance()
        instance.onAppStart(storage, DEFAULT_HANDLE)

        // reminder: the previous test methods ensure that these are the only files being created
        val contentsBefore = allFiles.filter { it.isFile }.associateWith { it.readBytes() }
        instance.encrypt(storage, DEFAULT_PASSWORD, "hello".toByteArray())
        val contentsAfter = allFiles.filter { it.isFile }.associateWith { it.readBytes() }

        // sanity checks
        assertThat(contentsBefore.size).isEqualTo(contentsAfter.size)
        assertThat(contentsBefore.keys).containsExactlyElementsOf(contentsAfter.keys)

        // check the files that we expect to change
        changingFiles.filter { it.isFile }.forEach { file ->
            assertThat(contentsBefore[file]).isNotEqualTo(contentsAfter[file])
        }

        // check the files that we expect to NOT change
        val notExpectedToChange = allFiles.toSet() - changingFiles.toSet()
        notExpectedToChange.filter { it.isFile }.forEach { file ->
            if (file.isFile) {
                assertThat(contentsBefore[file]).isEqualTo(contentsAfter[file])
            }
        }
    }

    @Test
    fun testHiddenSloth_whenCallingRatchet_thenExactlyTheCriticalFilesAreUpdated() {
        val instance = createInstance()
        instance.onAppStart(storage, DEFAULT_HANDLE)

        // reminder: the previous test methods ensure that these are the only files being created
        val contentsBefore = allFiles.filter { it.isFile }.associateWith { it.readBytes() }
        instance.ratchet(storage)
        val contentsAfter = allFiles.filter { it.isFile }.associateWith { it.readBytes() }

        // sanity checks
        assertThat(contentsBefore.size).isEqualTo(contentsAfter.size)
        assertThat(contentsBefore.keys).containsExactlyElementsOf(contentsAfter.keys)

        // check the files that we expect to change
        changingFiles.filter { it.isFile }.forEach { file ->
            assertThat(contentsBefore[file]).isNotEqualTo(contentsAfter[file])
        }

        // check the files that we expect to NOT change
        val notExpectedToChange = allFiles.toSet() - changingFiles.toSet()
        notExpectedToChange.filter { it.isFile }.forEach { file ->
            if (file.isFile) {
                assertThat(contentsBefore[file]).isEqualTo(contentsAfter[file])
            }
        }
    }

    private fun createInstance(payloadLength: Int = defaultPayloadLength) = HiddenSlothImpl(
        params = HiddenSlothParams(payloadMaxLength = payloadLength),
        secureElement = createSecureElementOrSkip(context),
        pwHash = Pbkdf2PwHash(),
    )
}
