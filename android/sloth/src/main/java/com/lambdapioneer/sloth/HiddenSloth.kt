package com.lambdapioneer.sloth

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.impl.HiddenSlothCachedSecrets
import com.lambdapioneer.sloth.impl.HiddenSlothImpl
import com.lambdapioneer.sloth.impl.OffsetAndLength
import com.lambdapioneer.sloth.storage.NamespaceableStorage
import com.lambdapioneer.sloth.storage.ReadableStorage
import com.lambdapioneer.sloth.storage.WriteableStorage
import javax.crypto.AEADBadTagException

@RequiresApi(Build.VERSION_CODES.P)
class HiddenSloth internal constructor(
    private val impl: HiddenSlothImpl,
    private val identifier: String,
) {
    private var isInitialized = false

    init {
        SlothLib.requireValidKeyIdentifier(identifier)
    }

    /**
     * This method checks whether the storage already exists, and creates it if necessary. This must
     * be called before any other method is being used.
     */
    fun <STORAGE> ensureStorage(
        storage: STORAGE,
    ) where STORAGE : WriteableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        if (!hasStorage(storage)) {
            val namespacedHandle = identifierToHandle(identifier)
            impl.init(namespacedStorage, namespacedHandle)
        }
        isInitialized = true
    }

    /**
     * Indicates whether the storage exists. Checking this should not be necessary and instead
     * [ensureStorage] should be called on every app start.
     */
    @VisibleForTesting
    internal fun <STORAGE> hasStorage(
        storage: STORAGE,
    ): Boolean where STORAGE : ReadableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        val namespacedStorage = storage.getOrCreateNamespace(identifier)
        return impl.exists(namespacedStorage)
    }

    /**
     * Removes the entire storage. Calling this method should not be neccessary in production apps
     * as they always should call [ensureStorage] during start-up and not dynamically delete
     * storage, as this can be an indicator for active usage.
     */
    fun <STORAGE> removeStorage(
        storage: STORAGE,
    ) where STORAGE : WriteableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        storage.deleteNamespace(identifier)
    }

    /**
     * Performs the Ratchet operation for the given storage. This should be called at least once
     * between each opportunity where the adversary can capture a snapshot to ensure
     * multi-snapshot resistance. For single-snapshot threat models this method is not required.
     */
    fun <STORAGE> ratchet(
        storage: STORAGE,
    ) where STORAGE : WriteableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        check(isInitialized)
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        impl.ratchet(namespacedStorage)
    }

    /**
     * Encrypts the given [data] under the provided [pw] using the [storage] namespaced to
     * [identifier]. The storage must have been previous initialized using [ensureStorage].
     */
    fun <STORAGE> encryptToStorage(
        pw: String,
        data: ByteArray,
        storage: STORAGE,
    ) where STORAGE : WriteableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        check(isInitialized)
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        impl.encrypt(namespacedStorage, pw, data)
    }

    /**
     * Authenticates and decrypts the entire storage using the provided password. If the password
     * does not unlock the storage, a [SlothDecryptionFailed] exception is thrown.
     *
     * Note that failure to decrypt can mean that (a) no data was ever encrypted,
     * (b) the password is wrong, or (c) the storage has been tampered with.
     */
    fun <STORAGE> decryptFromStorage(
        pw: String,
        storage: STORAGE,
    ): ByteArray where STORAGE : ReadableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        check(isInitialized)
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        try {
            return impl.decrypt(namespacedStorage, pw)
        } catch (e: AEADBadTagException) {
            throw SlothDecryptionFailed(
                message = "Decryption failed for key $identifier. This might mean there was never any user data stored.",
                cause = e
            )
        }
    }

    fun <STORAGE> computeCachedSecrets(
        pw: String,
        storage: STORAGE,
        authenticateStorage: Boolean = true,
    ): HiddenSlothCachedSecrets where STORAGE : ReadableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        check(isInitialized)
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        try {
            if (authenticateStorage) {
                impl.authenticate(namespacedStorage)
            }
            return impl.prepareCachedSecrets(namespacedStorage, pw)
        } catch (e: AEADBadTagException) {
            throw SlothDecryptionFailed(
                message = "Decryption failed for key $identifier. This might mean there was never any user data stored.",
                cause = e
            )
        }
    }

    /**
     * Uses the [HiddenSlothCachedSecrets] received from [computeCachedSecrets] to decrypt the
     * ciphertext from [storage].
     *
     * Note that with this method, the decryption is not authenticated. However, the
     * [computeCachedSecrets] method by default authenticates the storage. So, given there are no
     * intermediate changes, these calls are safe and efficient.
     *
     * If [decryptionOffsetAndLength] is provided, only the ciphertext in the range specified by
     * [decryptionOffsetAndLength] is decrypted. Note that in this case the decryption is not
     * authenticated.The offset and length must be aligned at AES block boundaries (16 bytes).
     * However, it will have been authenticated beforehand when calling [hiddenSlothCacheSecrets].
     */
    fun <STORAGE> decryptFromStorageWithCachedSecrets(
        cachedSecrets: HiddenSlothCachedSecrets,
        storage: STORAGE,
        decryptionOffsetAndLength: OffsetAndLength? = null,
    ): ByteArray where STORAGE : ReadableStorage, STORAGE : NamespaceableStorage<STORAGE> {
        check(isInitialized)
        val namespacedStorage = storage.getOrCreateNamespace(identifier)

        return impl.decrypt(
            storage = namespacedStorage,
            pw = null,
            cachedSecrets = cachedSecrets,
            decryptionOffsetAndLength = decryptionOffsetAndLength
        )
    }

    private fun identifierToHandle(identifier: String) =
        "__hiddensloth__$identifier".encodeToByteArray()
}
