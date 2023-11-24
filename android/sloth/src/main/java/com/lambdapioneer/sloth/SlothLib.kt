package com.lambdapioneer.sloth

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.crypto.Pbkdf2PwHash
import com.lambdapioneer.sloth.crypto.PwHash
import com.lambdapioneer.sloth.impl.HiddenSlothImpl
import com.lambdapioneer.sloth.impl.HiddenSlothParams
import com.lambdapioneer.sloth.impl.LongSlothImpl
import com.lambdapioneer.sloth.impl.LongSlothParams
import com.lambdapioneer.sloth.secureelement.DefaultSecureElement
import com.lambdapioneer.sloth.secureelement.SecureElement
import com.lambdapioneer.sloth.storage.SlothStorage
import java.time.Duration

/**
 * The main class of the Sloth library that provides the functionality to create and derive keys.
 * This class is thread-safe.
 *
 * The library is initialized by calling [init]. This method must be called before any other method.
 *
 * @param pwHash The password hashing implementation to use. Defaults to [Pbkdf2PwHash].
 */
@RequiresApi(Build.VERSION_CODES.P)
class SlothLib(private val pwHash: PwHash = Pbkdf2PwHash()) {

    private var isInitialized: Boolean = false

    private var secureElement: SecureElement = DefaultSecureElement()

    /**
     * Initializes the library. This method must be called before any other method. Throws an
     * [UnsupportedOperationException] if the device does not support Sloth (e.g. because it does
     * not have a secure element or the Android version is too low).
     */
    fun init(context: Context) {
        check(!isInitialized)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw UnsupportedOperationException("Sloth is not supported on this device, because it requires at least Android version P (9, API 26).")
        }

        if (!secureElement.isAvailable(context)) {
            throw UnsupportedOperationException("Sloth is not supported on this device, because it has no Secure Element.")
        }

        isInitialized = true
    }

    /**
     * Creates a new instance of [LongSloth] for the given [identifier] and [params]. This instance
     * will use the provided [storage] for all operations which will be namespaced inside based
     * on the [identifier].
     */
    fun getLongSlothInstance(
        identifier: String,
        storage: SlothStorage,
        params: LongSlothParams,
    ): LongSloth {
        val impl = LongSlothImpl(
            params = params,
            secureElement = secureElement,
            pwHash = pwHash,
        )
        return LongSloth(impl = impl, identifier = identifier, storage = storage)
    }

    /**
     * Creates a new instance of [HiddenSloth] for the given [identifier] and [params]. This
     * instance will use the provided [storage] for all operations which will be namespaced inside
     * based ogn the [identifier].
     */
    fun getHiddenSlothInstance(
        identifier: String,
        storage: SlothStorage,
        params: HiddenSlothParams,
    ): HiddenSloth {
        val impl = HiddenSlothImpl(
            params = params,
            secureElement = secureElement,
            pwHash = pwHash,
        )
        return HiddenSloth(impl = impl, identifier = identifier, storage = storage)
    }

    /**
     * Determines the parameter L for the given [targetDuration]. This method is intended to be
     * used for benchmarking purposes and determining the parameter L for a given device.
     */
    fun benchmarkParameter(targetDuration: Duration = Duration.ofSeconds(1)): SlothBenchmarkResult {
        val slothBenchmark = SlothParameterBenchmark(secureElement = secureElement)
        return slothBenchmark.determineParameter(targetDuration)
    }

    /**
     * Sets the [SecureElement] to use for testing. This method is only intended for testing.
     * Throws an [IllegalStateException] if the library is already initialized.
     */
    @VisibleForTesting
    internal fun setSecureElementForTesting(secureElement: SecureElement) {
        if (isInitialized) {
            throw IllegalStateException("Cannot set Secure Element after initialization.")
        }
        this.secureElement = secureElement
    }

    companion object {

        /**
         * Checks if the given identifier is valid. Throws an [IllegalArgumentException] if it is
         * not valid. Valid identifiers must only contain alphanumeric characters, dashes and
         * underscores, i.e. the regex "[a-zA-Z0-9-_]+".
         */
        fun requireValidKeyIdentifier(identifier: String) {
            if (!identifier.matches(Regex("[a-zA-Z0-9-_]+"))) {
                throw SlothBadIdentifier("The identifier must only contain alphanumeric characters, dashes and underscores.")
            }
        }
    }
}
