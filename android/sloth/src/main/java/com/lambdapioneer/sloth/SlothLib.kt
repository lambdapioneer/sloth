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
     * Creates a new instance of [LongSloth] for the given [identifier] and [params].
     */
    fun getLongSlothInstance(
        identifier: String,
        params: LongSlothParams,
    ): LongSloth {
        val impl = LongSlothImpl(
            params = params,
            secureElement = secureElement,
            pwHash = pwHash,
        )
        return LongSloth(impl = impl, identifier = identifier)
    }

    /**
     * Creates a new instance of [HiddenSloth] for the given [identifier] and [params].
     */
    fun getHiddenSlothInstance(
        identifier: String,
        params: HiddenSlothParams,
    ): HiddenSloth {
        val impl = HiddenSlothImpl(
            params = params,
            secureElement = secureElement,
            pwHash = pwHash,
        )
        return HiddenSloth(impl = impl, identifier = identifier)
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
