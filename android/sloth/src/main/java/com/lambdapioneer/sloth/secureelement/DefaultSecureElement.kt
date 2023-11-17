package com.lambdapioneer.sloth.secureelement

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.lambdapioneer.sloth.utils.secureRandomBytes
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec

@RequiresApi(Build.VERSION_CODES.P)
open class DefaultSecureElement : SecureElement {

    override fun isAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    }

    override fun aesCtrGenKey(keyHandle: KeyHandle) {
        require(keyHandle.handleString.isNotEmpty())

        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyHandle.handleString,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setIsStrongBoxBacked(true) // Forces use of secure element
            setBlockModes(KeyProperties.BLOCK_MODE_CTR)
            setKeySize(256)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setRandomizedEncryptionRequired(false) // to allow setting IV
            build()
        }

        val keyGen =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE_NAME)
        keyGen.init(keyGenSpec)
        keyGen.generateKey()
    }

    override fun aesCtrGenIv(): ByteArray = secureRandomBytes(length = AES_CTR_IV_LEN)

    override fun aesCtrEncrypt(keyHandle: KeyHandle, iv: ByteArray, data: ByteArray): ByteArray {
        require(keyHandle.handleString.isNotEmpty())
        require(iv.size == AES_CTR_IV_LEN)

        val key = KeyStore.getInstance(ANDROID_KEY_STORE_NAME).run {
            load(null)
            getKey(keyHandle.handleString, null)
        }
        checkNotNull(key)

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        return cipher.doFinal(data)
    }

    override fun aesCtrDecrypt(keyHandle: KeyHandle, iv: ByteArray, data: ByteArray): ByteArray {
        require(keyHandle.handleString.isNotEmpty())
        require(iv.size == AES_CTR_IV_LEN)
        require(data.isNotEmpty())

        val key = KeyStore.getInstance(ANDROID_KEY_STORE_NAME).run {
            load(null)
            getKey(keyHandle.handleString, null)
        }
        checkNotNull(key)

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        return cipher.doFinal(data)
    }

    override fun hmacGenKey(keyHandle: KeyHandle) {
        require(keyHandle.handleString.isNotEmpty())
        val keyLengthInBytes = HMAC_KEY_LENGTH_LONG

        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyHandle.handleString,
            KeyProperties.PURPOSE_SIGN
        ).run {
            setIsStrongBoxBacked(true) // Forces use of secure element
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

    override fun hmacDerive(keyHandle: KeyHandle, data: ByteArray): ByteArray {
        require(keyHandle.handleString.isNotEmpty())
        require(data.isNotEmpty())

        val key = KeyStore.getInstance(ANDROID_KEY_STORE_NAME).run {
            load(null)
            getKey(keyHandle.handleString, null)
        }
        checkNotNull(key)

        val mac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
        mac.init(key)
        return mac.doFinal(data)
    }

    companion object {
        private const val ANDROID_KEY_STORE_NAME = "AndroidKeyStore"
        const val AES_CTR_IV_LEN = 16
        const val HMAC_KEY_LENGTH_SHORT = 16
        const val HMAC_KEY_LENGTH_LONG = 32
    }
}
