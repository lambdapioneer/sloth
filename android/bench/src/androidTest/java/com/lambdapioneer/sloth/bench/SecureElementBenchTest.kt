package com.lambdapioneer.sloth.bench

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.secureelement.KeyHandle
import com.lambdapioneer.sloth.testing.KIB
import com.lambdapioneer.sloth.testing.createSecureElementOrSkip
import com.lambdapioneer.sloth.utils.secureRandomBytes
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

private const val TAG = "SecureElementBenchTest"

@RunWith(AndroidJUnit4::class)
class SecureElementBenchTest {

    @Rule
    @JvmField
    var ruleTestName = TestName()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val inputSizes: Array<Long> = arrayOf(
        ceilToNextBlockSize(1 * KIB),
        ceilToNextBlockSize((SQRT_10 * 1 * KIB).toInt()),
        ceilToNextBlockSize(10 * KIB),
        ceilToNextBlockSize((SQRT_10 * 10 * KIB).toInt()),
        ceilToNextBlockSize(100 * KIB),
        ceilToNextBlockSize((SQRT_10 * 100 * KIB).toInt()),
    )

    @Test
    @Ignore("not relevant for the paper; we use HmacLong")
    fun test_benchmarkIncreasingInputSize_AesCtr() {
        val instance = createSecureElementOrSkip(context)
        val keyHandle = KeyHandle("test_benchmarkIncreasingInputSize_AesCtr")
        val iv = instance.aesCtrGenIv()

        instance.aesCtrGenKey(keyHandle)

        for (i in 0 until ITERATIONS) {
            for (inputSize in inputSizes) {
                val input = secureRandomBytes(inputSize.toInt())
                val timeMs = measureTimeMillis {
                    instance.aesCtrDecrypt(keyHandle, iv, input)
                }
                Log.i(TAG, "${ruleTestName.methodName} inputSize=$inputSize timeMs=$timeMs")
            }
        }
    }

    @Test
    fun test_benchmarkIncreasingInputSize_hmacLong() =
        internalRunHmacBenchmark(name = ruleTestName.methodName)

    private fun internalRunHmacBenchmark(name: String) {
        val instance = createSecureElementOrSkip(context)
        val keyHandle = KeyHandle(name)
        instance.hmacGenKey(keyHandle = keyHandle)

        for (i in 0 until ITERATIONS) {
            for (inputSize in inputSizes) {
                val input = secureRandomBytes(inputSize.toInt())
                val timeMs = measureTimeMillis {
                    instance.hmacDerive(keyHandle, input)
                }
                Log.i(
                    TAG,
                    "$name inputSize=$inputSize timeMs=$timeMs"
                )
            }
        }
    }
}
