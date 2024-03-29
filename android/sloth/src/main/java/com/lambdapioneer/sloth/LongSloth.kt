package com.lambdapioneer.sloth

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.lambdapioneer.sloth.impl.LongSlothImpl
import com.lambdapioneer.sloth.storage.SlothStorage

@SuppressLint("VisibleForTests")
@RequiresApi(Build.VERSION_CODES.P)
class LongSloth internal constructor(
    private val impl: LongSlothImpl,
    private val identifier: String,
    private val storage: SlothStorage,
) {

    init {
        SlothLib.requireValidKeyIdentifier(identifier)
    }

    /**
     * This method should be called on every app start. It ensures that
     * the storage is initialized and its last-modified timestamps do not
     * leak usage information.
     */
    fun onAppStart() {
        val namespacedHandle = identifierToHandle(identifier)
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        impl.onAppStart(storage = namespacedStorage, h = namespacedHandle)
    }

    /**
     * Creates a new key with the given identifier and password.
     *
     * @param pw The password to use for the key derivation.
     * @param outputLengthBytes The length of the output key in bytes. Defaults to 32 bytes.
     */
    fun createNewKey(
        pw: CharArray,
        outputLengthBytes: Int = 32,
    ): ByteArray {
        val namespacedHandle = identifierToHandle(identifier)
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        return impl.keyGen(
            storage = namespacedStorage,
            pw = pw,
            h = namespacedHandle,
            outputLengthBytes = outputLengthBytes,
        )
    }

    /**
     * Creates a new key with the given identifier and password.
     *
     * @param pw The password to use for the key derivation.
     * @param outputLengthBytes The length of the output key in bytes. Defaults to 32 bytes.
     */
    fun createNewKey(
        pw: String,
        outputLengthBytes: Int = 32,
    ): ByteArray = createNewKey(
        pw = pw.toCharArray(),
        outputLengthBytes = outputLengthBytes,
    )

    /**
     * Derives the secret for the given key identifier and password.
     *
     * @param pw The password to use for the key derivation.
     * @param outputLengthBytes The length of the output key in bytes. Defaults to 32 bytes.
     */
    fun deriveForExistingKey(
        pw: CharArray,
        outputLengthBytes: Int = 32,
    ): ByteArray {
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        return impl.derive(
            storage = namespacedStorage,
            pw = pw,
            outputLengthBytes = outputLengthBytes,
        )
    }

    /**
     * Derives the secret for the given key identifier and password.
     *
     * @param pw The password to use for the key derivation.
     * @param outputLengthBytes The length of the output key in bytes. Defaults to 32 bytes.
     */
    fun deriveForExistingKey(
        pw: String,
        outputLengthBytes: Int = 32,
    ): ByteArray = deriveForExistingKey(
        pw = pw.toCharArray(),
        outputLengthBytes = outputLengthBytes,
    )

    /**
     * Checks the key with [identifier] exists.
     */
    fun hasKey(): Boolean {
        val namespacedStorage = storage.getOrCreateNamespace(identifier)
        return impl.exists(namespacedStorage)
    }

    /**
     * Deletes the key with the given identifier. If the key does not exist, this method does nothing.
     */
    fun deleteKey() {
        storage.deleteNamespace(identifier)
    }

    private fun identifierToHandle(identifier: String) =
        "__longsloth__$identifier".encodeToByteArray()
}
