package com.lambdapioneer.sloth.testing

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.secureelement.DefaultSecureElement
import com.lambdapioneer.sloth.secureelement.KeyHandle
import javax.crypto.KeyGenerator


/**
 * Same as [DefaultSecureElement], but without enforcing StrongBox usage. Hence, we can use it in
 * local tests against the emulator.
 *
 * (!) IMPORTANT: This class is only for testing. It should never be used in production code.
 */
@VisibleForTesting
@RequiresApi(Build.VERSION_CODES.P)
class MockedSecureElement : DefaultSecureElement() {

    init {
        Log.w("Sloth", "Using MockedSecureElement; it should only be used for testing!")
    }

    override fun isAvailable(context: Context): Boolean {
        return true
    }

    override fun aesCtrGenKey(keyHandle: KeyHandle) {
        require(keyHandle.handleString.isNotEmpty())

        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyHandle.handleString,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setBlockModes(KeyProperties.BLOCK_MODE_CTR)
            setKeySize(256)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setRandomizedEncryptionRequired(false) // to allow setting IV
            build()
        }

        val keyGen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE_NAME
        )
        keyGen.init(keyGenSpec)
        keyGen.generateKey()
    }

    override fun hmacGenKey(keyHandle: KeyHandle) {
        require(keyHandle.handleString.isNotEmpty())
        val keyLengthInBytes = HMAC_KEY_LENGTH_LONG

        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyHandle.handleString,
            KeyProperties.PURPOSE_SIGN
        ).run {
            setKeySize(keyLengthInBytes * 8)
            build()
        }

        val keyGen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            ANDROID_KEY_STORE_NAME
        )
        keyGen.init(keyGenSpec)
        keyGen.generateKey()
    }

    companion object {
        private const val ANDROID_KEY_STORE_NAME = "AndroidKeyStore"
    }
}
