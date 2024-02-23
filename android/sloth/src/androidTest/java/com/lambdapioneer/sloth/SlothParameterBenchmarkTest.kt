package com.lambdapioneer.sloth;

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.storage.OnDiskStorage
import com.lambdapioneer.sloth.testing.createSecureElementOrSkip
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration

// The tests in this class probably need some tuning to pass on a more diverse set of devices
@RunWith(AndroidJUnit4::class)
class SlothParameterBenchmarkTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testTargetOfHundredMillis_whenBenchmark_thenResultingRuntimeIsSlightlyLarger() =
        internal_whenBenchmark_thenResultingRuntimeIsSlightlyLarger(
            targetDuration = Duration.ofMillis(100)
        )

    @Test
    fun testTargetOfHundredMillis_whenBenchmarkingMultipleTimes_thenResultsAreSimilar() =
        internal_whenBenchmarkingMultipleTimes_thenResultsAreSimilar(
            targetDuration = Duration.ofMillis(100)
        )

    @Test
    fun testTargetOfHundredMillis_whenBenchmarking_thenTotalWaitingTimeIsReasonable() =
        internal_whenBenchmarking_thenTotalWaitingTimeIsReasonable(
            targetDuration = Duration.ofMillis(100)
        )

    @Test
    fun testTargetOfOneSecond_whenBenchmark_thenResultingRuntimeIsSlightlyLarger() =
        internal_whenBenchmark_thenResultingRuntimeIsSlightlyLarger(
            targetDuration = Duration.ofSeconds(1)
        )

    @Test
    fun testTargetOfOneSecond_whenBenchmarkingMultipleTimes_thenResultsAreSimilar() =
        internal_whenBenchmarkingMultipleTimes_thenResultsAreSimilar(
            targetDuration = Duration.ofSeconds(1)
        )

    @Test
    fun testTargetOfOneSecond_whenBenchmarking_thenTotalWaitingTimeIsReasonable() =
        internal_whenBenchmarking_thenTotalWaitingTimeIsReasonable(
            targetDuration = Duration.ofSeconds(1)
        )

    private fun internal_whenBenchmark_thenResultingRuntimeIsSlightlyLarger(
        targetDuration: Duration,
    ) {
        // We want to skip instead of using the mocked SE, as we need the real one for the
        // verification of the runtime using the normal `SlothLib` API below.
        val instance = getInstance(useMockedSecureElementOtherwise = false)

        val benchmarkResult = instance.determineParameter(targetDuration)

        assertThat(benchmarkResult.duration).isGreaterThanOrEqualTo(targetDuration)
        assertThat(benchmarkResult.l).isGreaterThanOrEqualTo(SlothParameterBenchmark.L_MINIMUM)

        val slothLib = SlothLib()
        slothLib.init(context)
        val longSloth = slothLib.getLongSlothInstance(
            identifier = "test",
            storage = OnDiskStorage(context),
            params = benchmarkResult.toParams()
        )

        val waitingTime = measureWaitingTime {
            longSloth.createNewKey(pw = "passphrase".toCharArray())
        }
        assertThat(waitingTime).isGreaterThan(targetDuration)
        assertThat(waitingTime).isLessThan(targetDuration.multipliedBy(5))
    }

    private fun internal_whenBenchmarkingMultipleTimes_thenResultsAreSimilar(
        targetDuration: Duration,
    ) {
        val instance = getInstance()

        val iterations = 3
        val benchmarkResults = (0 until iterations).map {
            val x = instance.determineParameter(targetDuration)
            assertThat(x.duration).isGreaterThanOrEqualTo(targetDuration)
            assertThat(x.l).isGreaterThanOrEqualTo(SlothParameterBenchmark.L_MINIMUM)
            x
        }

        // do not allow more than 500ms (or 50%) duration spread
        val spreadDuration =
            benchmarkResults.maxOf { it.duration } - benchmarkResults.minOf { it.duration }
        val maxDuration = benchmarkResults.maxOf { it.duration }
        assertThat(spreadDuration).isLessThan(maxDuration.dividedBy(2))

        // do not allow more than 25% parameter spread
        val spreadParameterL = benchmarkResults.maxOf { it.l } - benchmarkResults.minOf { it.l }
        val maxParameterL = benchmarkResults.maxOf { it.l }
        assertThat(spreadParameterL).isLessThan(maxParameterL / 4)
    }

    private fun internal_whenBenchmarking_thenTotalWaitingTimeIsReasonable(
        targetDuration: Duration,
    ) {
        val instance = getInstance()
        val reasonableWaitingTime = targetDuration.multipliedBy(10)

        val waitingTime = measureWaitingTime {
            instance.determineParameter(targetDuration)
        }
        assertThat(waitingTime).isLessThanOrEqualTo(reasonableWaitingTime)
    }

    private fun measureWaitingTime(block: () -> Unit): Duration {
        val start = System.currentTimeMillis()
        block()
        val end = System.currentTimeMillis()
        return Duration.ofMillis(end - start)
    }

    private fun getInstance(useMockedSecureElementOtherwise: Boolean = true): SlothParameterBenchmark {
        val secureElement = createSecureElementOrSkip(
            context = context,
            useMockedSecureElementOtherwise = useMockedSecureElementOtherwise,
        )
        return SlothParameterBenchmark(secureElement)
    }
}
