package com.lambdapioneer.sloth.bench

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.impl.LongSlothImpl
import com.lambdapioneer.sloth.impl.LongSlothParams
import com.lambdapioneer.sloth.pwhash_libsodium.LibSodiumArgon2PwHash
import com.lambdapioneer.sloth.pwhash_libsodium.LibSodiumArgon2PwHashParams
import com.lambdapioneer.sloth.secureelement.SecureElement
import com.lambdapioneer.sloth.storage.InMemoryStorage
import com.lambdapioneer.sloth.testing.createSecureElementOrSkip
import com.lambdapioneer.sloth.testing.getDeviceName
import com.lambdapioneer.sloth.testing.getParameterForDevice
import com.lambdapioneer.sloth.utils.LogTracer
import com.lambdapioneer.sloth.utils.NoopTracer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LongSlothBenchTest {

    @Rule
    @JvmField
    var ruleTestName = TestName()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val defaultKeyHandle = "handle".encodeToByteArray()
    private val defaultPassword = "password"
    private val defaultOutputKeyLength = 256 / 8

    @Test
    fun testBenchLongSloth_withOwasp_small() {
        val secureElement = createSecureElementOrSkip(context)
        val l = getParameterForDevice("small", getDeviceName())

        internalRunBenchmark(
            secureElement = secureElement,
            pwHashParams = LibSodiumArgon2PwHashParams.OWASP,
            parameterL = l,
        )
    }

    @Test
    fun testBenchLongSloth_withOwasp_large() {
        val secureElement = createSecureElementOrSkip(context)
        val l = getParameterForDevice("large", getDeviceName())

        internalRunBenchmark(
            secureElement = secureElement,
            pwHashParams = LibSodiumArgon2PwHashParams.OWASP,
            parameterL = l,
        )
    }

    private fun internalRunBenchmark(
        secureElement: SecureElement,
        pwHashParams: LibSodiumArgon2PwHashParams,
        parameterL: Int,
    ) {
        val methodName = ruleTestName.methodName

        val storage = InMemoryStorage()

        // generate key once and don't log about it
        val keyGenInstance = LongSlothImpl(
            params = LongSlothParams(l = parameterL),
            secureElement = secureElement,
            pwHash = LibSodiumArgon2PwHash(pwHashParams),
            tracer = NoopTracer(),
        )
        keyGenInstance.keyGen(
            storage = storage,
            pw = defaultPassword.toCharArray(),
            h = defaultKeyHandle,
            outputLengthBytes = defaultOutputKeyLength
        )

        // then derive multiple times and log the traces to the console
        for (i in 0 until ITERATIONS) {
            val tracer = LogTracer(methodName)
            tracer.addKeyValue("iteration", i.toString())

            val deriveInstance = LongSlothImpl(
                params = LongSlothParams(l = parameterL),
                secureElement = secureElement,
                pwHash = LibSodiumArgon2PwHash(pwHashParams),
                tracer = NoopTracer(),
            )
            deriveInstance.derive(
                storage = storage,
                pw = defaultPassword.toCharArray(),
                outputLengthBytes = defaultOutputKeyLength
            )
        }
    }
}
