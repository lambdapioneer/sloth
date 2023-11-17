package com.lambdapioneer.sloth.storage

import com.lambdapioneer.sloth.SlothStorageKeyNotFound

/**
 * A storage implementation that is backed by a [HashMap].
 */
class InMemoryStorage private constructor(
    private val namespace: String,
    private val data: HashMap<String, ByteArray>,
) : ReadableStorage, WriteableStorage, NamespaceableStorage<InMemoryStorage> {

    constructor() : this(namespace = "", data = HashMap())

    override fun put(key: String, value: ByteArray) {
        val transformedKey = transformKey(key)
        data[transformedKey] = value
    }

    override fun get(key: String): ByteArray {
        val transformedKey = transformKey(key)
        return data[transformedKey] ?: throw SlothStorageKeyNotFound()
    }

    override fun delete(key: String) {
        data.remove(key)
    }

    /**
     * Creates a new [InMemoryStorage] instance with the given name as a sub-namespace of this
     * instance, but that references the same underlying data, i.e. the same [HashMap].
     */
    override fun getOrCreateNamespace(name: String): InMemoryStorage {
        return InMemoryStorage(namespace = transformKey(name), data = data)
    }

    override fun deleteNamespace(name: String) {
        val affectedKeys = data.keys.filter { it.startsWith(name) }
        affectedKeys.forEach { data.remove(it) }
    }

    private fun transformKey(key: String) = "$namespace/$key"
}

