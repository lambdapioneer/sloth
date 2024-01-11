package com.lambdapioneer.sloth

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.impl.LongSlothParams
import com.lambdapioneer.sloth.secureelement.KeyHandle
import com.lambdapioneer.sloth.secureelement.SecureElement
import java.time.Duration

/**
 * A benchmark for determining a suitable L parameter for [HiddenSloth] for the current device.
 */
@RequiresApi(Build.VERSION_CODES.P)
class SlothParameterBenchmark(private val secureElement: SecureElement) {

    /**
     * Determines a suitable L parameter for [HiddenSloth] that likely results in a runtime of at
     * least [targetDuration].
     */
    fun determineParameter(targetDuration: Duration): SlothBenchmarkResult {
        val targetDurationNs = targetDuration.toNanos()
        var lastParameterL = L_START_VALUE
        var lastDurationNs = 0L

        // ensure an HMAC benchmark key
        val keyHandle = KeyHandle(BENCHMARK_KEY_HANDLE)
        secureElement.hmacGenKey(keyHandle = keyHandle)

        // keep looking until our last result is over the target duration
        outer@ while (lastDurationNs < targetDurationNs) {
            val measurements: MutableList<Long> = mutableListOf()

            // collect measurements for our current best guess
            for (i in 0 until INNER_ITERATIONS) {
                lastDurationNs = measure(keyHandle, lastParameterL.toInt())

                // if any duration is over the target, we stop; in particular once we hit the target
                // duration we do not want to spend 3 times the target duration
                if (lastDurationNs >= targetDurationNs) break@outer

                // average over the results
                measurements.add(lastDurationNs)
            }

            // scale L for the next iteration based on the measured average duration
            val averageDurationNs = measurements.average()
            val scaleUp = targetDurationNs / averageDurationNs * SCALE_UP_EXTRA
            lastParameterL *= scaleUp.coerceIn(SCALE_UP_RANGE_MIN..SCALE_UP_RANGE_MAX)
        }

        check(lastParameterL >= L_MINIMUM)
        check(lastDurationNs >= targetDurationNs)
        return SlothBenchmarkResult(
            l = lastParameterL.toInt(),
            duration = Duration.ofNanos(lastDurationNs)
        )
    }

    private fun measure(keyHandle: KeyHandle, l: Int): Long {
        val startNs = System.nanoTime()
        secureElement.hmacDerive(keyHandle, ByteArray(l))
        val endNs = System.nanoTime()
        return endNs - startNs
    }

    companion object {
        // chosen to be in the range of ~100 ms on a slow device (Pixel 3) and ~20 ms on a fast
        // device (Samsung S22)
        private const val L_START_VALUE = 2_000.0

        // a minimum value for L to ensure that if anything goes wrong with the benchmark, we don't
        // end up with a value that is way too small
        @VisibleForTesting
        internal const val L_MINIMUM = 2_000

        // the number of iterations to average over for each L
        private const val INNER_ITERATIONS = 3

        // the range to scale up L for the next iteration
        private const val SCALE_UP_RANGE_MIN = 1.0
        private const val SCALE_UP_RANGE_MAX = 50.0

        // on top of the calculated scale up we add this extra factor to compensate any noise in the
        // measurements and observed sub-linear behaviour for larger L that make the search take
        // longer than necessary otherwise; note that we still measure the final result at least
        // once to make sure we are over the target duration
        private const val SCALE_UP_EXTRA = 1.25

        // the handle for the benchmark key
        private const val BENCHMARK_KEY_HANDLE = "__SlothBenchmark"
    }
}

/**
 * The result of a benchmark run. Contains the L parameter and one measured actual runtime for this
 * parameter.
 */
data class SlothBenchmarkResult(
    val l: Int,
    val duration: Duration,
) {

    /**
     * Returns the [LongSlothParams] using the L parameter from this benchmark result.
     */
    fun toParams(): LongSlothParams {
        return LongSlothParams(l)
    }
}
