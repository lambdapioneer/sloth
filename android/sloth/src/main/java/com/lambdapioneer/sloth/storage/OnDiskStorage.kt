package com.lambdapioneer.sloth.storage

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.lambdapioneer.sloth.SlothStorageKeyNotFound
import java.io.File
import java.io.FileNotFoundException

/**
 * A storage implementation that is backed by files with in the [Context.getFilesDir]
 * folder.
 *
 * The [Context] is used to get the [Context.getFilesDir] folder and the [Context.filesDir] folder
 * is used as the base path for the storage. If a [customBasePath] is provided, that is used
 * instead.
 */
class OnDiskStorage(
    private val context: Context,
    private val customBasePath: File? = null,
) : SlothStorage {

    //
    // ReadableStorage
    //

    override fun get(key: String): ByteArray {
        try {
            return getFile(key).readBytes()
        } catch (e: FileNotFoundException) {
            throw SlothStorageKeyNotFound()
        }
    }

    //
    // WriteableStorage
    //

    override fun put(key: String, value: ByteArray) {
        getFile(key).writeBytes(value)
    }

    override fun delete(key: String) {
        getFile(key).delete()
    }

    override fun updateAllLastModifiedTimestamps() {
        basePath().walkBottomUp().forEach { file ->
            file.setLastModified(System.currentTimeMillis())
        }
    }

    //
    // NamespaceableStorage
    //

    override fun getOrCreateNamespace(name: String): OnDiskStorage {
        return OnDiskStorage(context = context, customBasePath = File(basePath(), name))
    }

    override fun deleteNamespace(name: String) {
        val dir = File(basePath(), name)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    @VisibleForTesting
    fun basePath(): File = customBasePath ?: defaultBasePath()

    private fun defaultBasePath() = File(context.filesDir, "sloth")

    /**
     * Returns the file for the given key. If the file does not exist, it is created.
     */
    private fun getFile(key: String): File {
        require(!key.contains("."))
        require(!key.contains("/"))
        require(!key.contains("\\"))

        // ensure that the directory exists
        val dir = basePath()
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return File(dir, key)
    }
}
