package com.lambdapioneer.sloth.secureelement

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import com.lambdapioneer.sloth.secureelement.DefaultSecureElement.Companion.HMAC_KEY_LENGTH_LONG
import com.lambdapioneer.sloth.secureelement.DefaultSecureElement.Companion.HMAC_KEY_LENGTH_SHORT
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

private const val TAG = "HKSOutput"

private const val KEY_STORE = "AndroidKeyStore"
private const val KEY_NAME_SE = "strong_box_key"
private const val KEY_NAME_DEFAULT = "default_key"


class HardwareKeySupport(private val context: Context) {

    fun isStrongBoxSupportSystemFeaturePresent(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            false
        } else {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        }
    }

    fun isSingleUseKeySupportSystemFeaturePresent(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            false
        } else {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_KEYSTORE_SINGLE_USE_KEY)
        }
    }

    fun isLimitedUseKeySupportSystemFeaturePresent(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            false
        } else {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_KEYSTORE_LIMITED_USE_KEY)
        }
    }

    fun canCreateStrongBoxKeyAes(blockMode: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val keyName = "${KEY_NAME_SE}_AES_${blockMode}"

        // key spec
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setIsStrongBoxBacked(true) // Forces use of secure element
            setBlockModes(blockMode)
            setKeySize(256)
            build()
        }

        // key creation
        try {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE)
            keyGen.init(keyGenSpec)
            keyGen.generateKey()

            val keyInfo = getKeyInfo(keyName)
            return keyInfo.isInsideSecureHardware
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            return false
        }
    }

    fun canCreateStrongBoxKeyHmac(lengthBytes: Int): Boolean {
        require(lengthBytes == HMAC_KEY_LENGTH_SHORT || lengthBytes == HMAC_KEY_LENGTH_LONG)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val keyName = "${KEY_NAME_SE}_HMAC_${lengthBytes}"

        // key spec
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_SIGN
        ).run {
            setIsStrongBoxBacked(true) // Forces use of secure element
            setKeySize(lengthBytes * 8)
            build()
        }

        // key creation
        try {
            val keyGen =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEY_STORE)
            keyGen.init(keyGenSpec)
            keyGen.generateKey()

            val keyInfo = getKeyInfo(keyName)
            return keyInfo.isInsideSecureHardware
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            return false
        }
    }

    fun isDefaultKeyGenerationHardwareBackedAes(blockMode: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val keyName = "${KEY_NAME_DEFAULT}_AES_${blockMode}"

        // key spec
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setBlockModes(blockMode)
            setKeySize(256)
            build()
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE)
        keyGen.init(keyGenSpec)
        keyGen.generateKey()

        val keyInfo = getKeyInfo(keyName)
        Log.d(TAG, "keyInfo.isInsideSecureHardware ${keyInfo.isInsideSecureHardware}")

        return keyInfo.isInsideSecureHardware
    }

    fun isDefaultKeyGenerationHardwareBackedHmac(lengthBytes: Int): Boolean {
        require(lengthBytes == HMAC_KEY_LENGTH_SHORT || lengthBytes == HMAC_KEY_LENGTH_LONG)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val keyName = "${KEY_NAME_DEFAULT}_HMAC_${lengthBytes}"

        // key spec
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_SIGN
        ).run {
            setKeySize(lengthBytes * 8)
            build()
        }

        // key creation
        try {
            val keyGen =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEY_STORE)
            keyGen.init(keyGenSpec)
            keyGen.generateKey()

            val keyInfo = getKeyInfo(keyName)
            return keyInfo.isInsideSecureHardware
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            return false
        }
    }

    fun canCreateStrongBoxKeyHmacWithLimitedUse(lengthBytes: Int): Boolean {
        require(lengthBytes == HMAC_KEY_LENGTH_SHORT || lengthBytes == HMAC_KEY_LENGTH_LONG)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val keyName = "${KEY_NAME_SE}_HMAC_${lengthBytes}"

        // key spec
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_SIGN
        ).run {
            setIsStrongBoxBacked(true) // Forces use of secure element
            setKeySize(lengthBytes * 8)
            setMaxUsageCount(10_000)
            build()
        }

        // key creation
        try {
            val keyGen =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEY_STORE)
            keyGen.init(keyGenSpec)
            keyGen.generateKey()

            val keyInfo = getKeyInfo(keyName)
            return keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getKeyInfo(keyName: String): KeyInfo {
        // retrieve key
        val key = KeyStore.getInstance(KEY_STORE).run {
            load(null)
            getKey(keyName, null)
        } as SecretKey

        // instantiate key info
        val keyFactory = SecretKeyFactory.getInstance(key.algorithm, KEY_STORE)
        return keyFactory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
    }
}
