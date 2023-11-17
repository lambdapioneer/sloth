package com.lambdapioneer.sloth

import android.os.Build
import androidx.annotation.RequiresApi
import com.lambdapioneer.sloth.impl.LongSlothImpl
import com.lambdapioneer.sloth.storage.NamespaceableStorage
import com.lambdapioneer.sloth.storage.ReadableStorage
import com.lambdapioneer.sloth.storage.WriteableStorage

@RequiresApi(Build.VERSION_CODES.P)
class LongSloth internal constructor(
    private val impl: LongSlothImpl,
    private val identifier: String,
) {

    init {
        SlothLib.requireValidKeyIdentifier(identifier)
    }

    /**
     * Creates a new key with the given identifier and password.
     *
     * @param pw The password to use for the key derivation.
     * @param storage The storage to use for the key derivation.
     * @param outputLengthBytes The length of the output key in bytes. Defaults to 32 bytes.
     */
    fun <STORAGE> createNewKey(
        pw: String,
        storage: STORAGE,
        outputLengthBytes: Int = 32,
    ): ByteArray where STORAGE : WriteableStorage, STORAGE : NamespaceableStorage<STORAGE> {
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
     * Derives the secret for the given key identifier and password.
     *
     * @param pw The password to use for the key derivation.
     * @param storage The storage to use for the key derivation.
     * @param outputLengthBytes The length of the output key in bytes. Defaults to 32 bytes.
     */
    fun <STORAGE> deriveForExistingKey(
        pw: String,
        storage: STORAGE,
        outputLengthBytes: Int = 32,
    ): ByteArray where STORAGE : ReadableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        return impl.derive(
            storage = namespacedStorage,
            pw = pw,
            outputLengthBytes = outputLengthBytes,
        )
    }

    /**
     * Checks the key with [identifier] exists.
     *
     * @param storage The storage to use.
     */
    fun <STORAGE> hasKey(
        storage: STORAGE,
    ): Boolean where STORAGE : ReadableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        val namespacedStorage = storage.getOrCreateNamespace(identifier)
        return impl.hasKey(namespacedStorage)
    }

    /**
     * Deletes the key with the given identifier. If the key does not exist, this method does nothing.
     *
     * @param storage The storage to use.
     */
    fun <STORAGE> deleteKey(
        storage: STORAGE,
    ) where STORAGE : WriteableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        storage.deleteNamespace(identifier)
    }

    private fun identifierToHandle(identifier: String) =
        "__longsloth__$identifier".encodeToByteArray()
}
