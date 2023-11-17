package com.lambdapioneer.sloth.bench

import android.security.keystore.KeyProperties
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.secureelement.DefaultSecureElement.Companion.HMAC_KEY_LENGTH_LONG
import com.lambdapioneer.sloth.secureelement.DefaultSecureElement.Companion.HMAC_KEY_LENGTH_SHORT
import com.lambdapioneer.sloth.secureelement.HardwareKeySupport
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith


private const val TAG = "HardwareKeySupportTest"

@RunWith(AndroidJUnit4::class)
class HardwareKeySupportTest {

    @Rule
    @JvmField
    var ruleTestName = TestName()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val instance = HardwareKeySupport(context)

    @Test
    fun testIsStrongBoxSupportSystemFeaturePresent() =
        internalTestTimed { instance.isStrongBoxSupportSystemFeaturePresent() }

    @Test
    fun testIsSingleUseKeySupportSystemFeaturePresent() =
        internalTestTimed { instance.isSingleUseKeySupportSystemFeaturePresent() }

    @Test
    fun testIsLimitedUseKeySupportSystemFeaturePresent() =
        internalTestTimed { instance.isLimitedUseKeySupportSystemFeaturePresent() }

    @Test
    fun testCanCreateStrongBoxKeyCtr() =
        internalTestTimed { instance.canCreateStrongBoxKeyAes(KeyProperties.BLOCK_MODE_CTR) }

    @Test
    fun testCanCreateStrongBoxKeyGcm() =
        internalTestTimed { instance.canCreateStrongBoxKeyAes(KeyProperties.BLOCK_MODE_GCM) }

    @Test
    fun testCanCreateStrongBoxKeyHmacShort() =
        internalTestTimed { instance.canCreateStrongBoxKeyHmac(lengthBytes = HMAC_KEY_LENGTH_SHORT) }

    @Test
    fun testCanCreateStrongBoxKeyHmacLong() =
        internalTestTimed { instance.canCreateStrongBoxKeyHmac(lengthBytes = HMAC_KEY_LENGTH_LONG) }

    @Test
    fun testIsDefaultKeyGenerationHardwareBackedCtr() =
        internalTestTimed { instance.isDefaultKeyGenerationHardwareBackedAes(KeyProperties.BLOCK_MODE_CTR) }

    @Test
    fun testIsDefaultKeyGenerationHardwareBackedGcm() =
        internalTestTimed { instance.isDefaultKeyGenerationHardwareBackedAes(KeyProperties.BLOCK_MODE_GCM) }

    @Test
    fun testIsDefaultKeyGenerationHardwareBackedHmacShort() =
        internalTestTimed { instance.isDefaultKeyGenerationHardwareBackedHmac(lengthBytes = HMAC_KEY_LENGTH_SHORT) }

    @Test
    fun testIsDefaultKeyGenerationHardwareBackedHmacLong() =
        internalTestTimed { instance.isDefaultKeyGenerationHardwareBackedHmac(lengthBytes = HMAC_KEY_LENGTH_LONG) }

    @Test
    fun testCanCreateStrongBoxKeyHmacLongWithLimitedUse() =
        internalTestTimed { instance.canCreateStrongBoxKeyHmacWithLimitedUse(lengthBytes = HMAC_KEY_LENGTH_LONG) }


    private fun internalTestTimed(functionUnderTest: () -> Boolean) {
        val name = ruleTestName.methodName

        val startNs = System.nanoTime()
        val result = functionUnderTest()
        val deltaMs = (System.nanoTime() - startNs) / 1_000_000.0

        Log.i(TAG, "$name $result $deltaMs")
    }
}
