package com.lambdapioneer.sloth.bench

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.impl.HiddenSlothImpl
import com.lambdapioneer.sloth.impl.HiddenSlothParams
import com.lambdapioneer.sloth.impl.LongSlothParams
import com.lambdapioneer.sloth.impl.OffsetAndLength
import com.lambdapioneer.sloth.pwhash_libsodium.LibSodiumArgon2PwHash
import com.lambdapioneer.sloth.pwhash_libsodium.LibSodiumArgon2PwHashParams
import com.lambdapioneer.sloth.secureelement.SecureElement
import com.lambdapioneer.sloth.storage.OnDiskStorage
import com.lambdapioneer.sloth.testing.MIB
import com.lambdapioneer.sloth.testing.createSecureElementOrSkip
import com.lambdapioneer.sloth.testing.getDeviceName
import com.lambdapioneer.sloth.testing.getParameterForDevice
import com.lambdapioneer.sloth.utils.LogTracer
import com.lambdapioneer.sloth.utils.NoopTracer
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.lang.Integer.min

private val DEFAULT_HANDLE = "handle".encodeToByteArray()
private const val DEFAULT_PASSWORD = "password"

private enum class HiddenSlothOperationUnderTest {
    INIT,
    RATCHET,
    DECRYPT,
    DECRYPT_WITH_CACHED_KEY,
    ENCRYPT,
}

@RunWith(AndroidJUnit4::class)
class HiddenSlothBenchTest {

    private val maxDataSize: Array<Long> = arrayOf(
        ceilToNextBlockSize(1 * MIB),
        ceilToNextBlockSize((SQRT_10 * 1 * MIB).toInt()),
        ceilToNextBlockSize(10 * MIB),
        ceilToNextBlockSize((SQRT_10 * 10 * MIB).toInt()),
        ceilToNextBlockSize(100 * MIB),
    )

    @Rule
    @JvmField
    var ruleTestName = TestName()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = OnDiskStorage(context)

    @Test
    fun testHiddenSloth_withSmall_withOpRatchet() {
        internalRunBenchmark(HiddenSlothOperationUnderTest.RATCHET)
    }

    @Test
    @Ignore("not used in paper")
    fun testHiddenSloth_withSmall_withOpInit() {
        internalRunBenchmark(HiddenSlothOperationUnderTest.INIT)
    }

    @Test
    @Ignore("not used in paper")
    fun testHiddenSloth_withSmall_withOpEncrypt() {
        internalRunBenchmark(HiddenSlothOperationUnderTest.ENCRYPT)
    }

    @Test
    fun testHiddenSloth_withSmall_withOpDecrypt() {
        internalRunBenchmark(HiddenSlothOperationUnderTest.DECRYPT)
    }

    @Test
    fun testHiddenSloth_withSmall_withOpDecryptWithCachedKey() {
        internalRunBenchmark(HiddenSlothOperationUnderTest.DECRYPT_WITH_CACHED_KEY)
    }

    private fun internalRunBenchmark(
        operationUnderTest: HiddenSlothOperationUnderTest,
    ) {
        val secureElement = createSecureElementOrSkip(context)
        val l = getParameterForDevice("small", getDeviceName())

        for (s in maxDataSize) {
            storage.basePath().deleteRecursively()
            internalRunBenchmarkForSize(
                secureElement = secureElement,
                pwHashParams = LibSodiumArgon2PwHashParams.OWASP,
                parameterL = l,
                parameterMaxSize = s.toInt(),
                operationUnderTest = operationUnderTest,
            )
        }
    }

    private fun internalRunBenchmarkForSize(
        secureElement: SecureElement,
        pwHashParams: LibSodiumArgon2PwHashParams,
        parameterL: Int,
        parameterMaxSize: Int,
        operationUnderTest: HiddenSlothOperationUnderTest,
    ) {
        val methodName = ruleTestName.methodName

        val params = HiddenSlothParams(
            payloadMaxLength = parameterMaxSize,
            longSlothParams = LongSlothParams(l = parameterL),
        )

        // initialize once (without logging)
        val hiddenSloth = HiddenSlothImpl(
            params = params,
            secureElement = secureElement,
            pwHash = LibSodiumArgon2PwHash(pwHashParams),
            tracer = NoopTracer(),
        )
        hiddenSloth.init(storage, DEFAULT_HANDLE)

        val maxSizeEncryptionPayload = ByteArray(parameterMaxSize)

        // if we test decryption, we ensure that we have previously encrypted data
        if (operationUnderTest == HiddenSlothOperationUnderTest.DECRYPT || operationUnderTest == HiddenSlothOperationUnderTest.DECRYPT_WITH_CACHED_KEY) {
            hiddenSloth.encrypt(
                storage = storage,
                pw = DEFAULT_PASSWORD.toCharArray(),
                data = maxSizeEncryptionPayload,
                tracer = NoopTracer(),
            )
        }

        // if we test decryption with cached secrets, we compute them once here
        val cachedSecrets =
            if (operationUnderTest == HiddenSlothOperationUnderTest.DECRYPT_WITH_CACHED_KEY) {
                hiddenSloth.computeCachedSecrets(
                    storage = storage,
                    pw = DEFAULT_PASSWORD.toCharArray(),
                )
            } else {
                null
            }

        // execute operation multiple times
        for (i in 0 until ITERATIONS) {
            val tracer = LogTracer(methodName)

            tracer.addKeyValue("iteration", i.toString())
            tracer.addKeyValue("l", parameterL.toString())
            tracer.addKeyValue("s", parameterMaxSize.toString())

            when (operationUnderTest) {
                HiddenSlothOperationUnderTest.INIT -> hiddenSloth.init(
                    storage = storage,
                    h = DEFAULT_HANDLE,
                    tracer = tracer
                )

                HiddenSlothOperationUnderTest.DECRYPT -> hiddenSloth.decrypt(
                    storage = storage,
                    pw = DEFAULT_PASSWORD.toCharArray(),
                    tracer = tracer
                )

                HiddenSlothOperationUnderTest.DECRYPT_WITH_CACHED_KEY -> {
                    val length = min(1 * MIB, maxSizeEncryptionPayload.size)
                    hiddenSloth.decrypt(
                        storage = storage,
                        pw = null,
                        tracer = tracer,
                        cachedSecrets = cachedSecrets!!,
                        decryptionOffsetAndLength = OffsetAndLength(0, length)
                    )
                }

                HiddenSlothOperationUnderTest.ENCRYPT -> hiddenSloth.encrypt(
                    storage = storage,
                    pw = DEFAULT_PASSWORD.toCharArray(),
                    data = maxSizeEncryptionPayload,
                    tracer = tracer
                )

                HiddenSlothOperationUnderTest.RATCHET -> hiddenSloth.ratchet(
                    storage = storage,
                    tracer = tracer
                )
            }
        }
    }
}
