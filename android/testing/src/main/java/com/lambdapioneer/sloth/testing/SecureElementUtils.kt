package com.lambdapioneer.sloth.testing

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.lambdapioneer.sloth.secureelement.DefaultSecureElement
import com.lambdapioneer.sloth.secureelement.HardwareKeySupport
import com.lambdapioneer.sloth.secureelement.SecureElement
import org.junit.Assume
import org.junit.AssumptionViolatedException

internal const val HMAC_KEY_LENGTH_LONG = 32

@SuppressLint("VisibleForTests")
@RequiresApi(Build.VERSION_CODES.P)
fun createSecureElementOrSkip(
    context: Context,
    useMockedSecureElementOtherwise: Boolean = true,
): SecureElement {
    try {
        Assume.assumeTrue(
            "Skipped because no secure element with AES_CTR support available",
            HardwareKeySupport(context).canCreateStrongBoxKeyAes(KeyProperties.BLOCK_MODE_CTR)
        )
        Assume.assumeTrue(
            "Skipped because no secure element with HMAC support available",
            HardwareKeySupport(context).canCreateStrongBoxKeyHmac(HMAC_KEY_LENGTH_LONG)
        )
    } catch (e: AssumptionViolatedException) {
        if (useMockedSecureElementOtherwise) {
            return MockedSecureElement()
        }
        throw e
    }
    return DefaultSecureElement()
}
