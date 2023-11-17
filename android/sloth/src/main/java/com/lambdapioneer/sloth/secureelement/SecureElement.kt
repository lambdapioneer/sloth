package com.lambdapioneer.sloth.secureelement

import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties.*
import androidx.annotation.RequiresApi


/**
 * Interface for an abstract [SecureElement] which provides both a [hmacDerive] functionality and
 * symmetric [aesCtrEncrypt] and [aesCtrDecrypt] methods including the respective key generation
 * methods.
 */
@RequiresApi(Build.VERSION_CODES.P)
interface SecureElement {
    fun isAvailable(context: Context): Boolean

    fun aesCtrGenKey(keyHandle: KeyHandle)

    fun aesCtrGenIv(): ByteArray

    fun aesCtrEncrypt(keyHandle: KeyHandle, iv: ByteArray, data: ByteArray): ByteArray

    fun aesCtrDecrypt(keyHandle: KeyHandle, iv: ByteArray, data: ByteArray): ByteArray

    fun hmacGenKey(keyHandle: KeyHandle)

    fun hmacDerive(keyHandle: KeyHandle, data: ByteArray): ByteArray
}
