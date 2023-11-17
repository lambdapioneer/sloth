package com.lambdapioneer.sloth.storage

/**
 * Different to the paper where it is handy to have an immutable storage, we assume that the storage
 * can be modified by the operations. This makes for more readable code and the same is true for
 * the SE state, as that lives outside the context of this process.
 */
interface WriteableStorage : ReadableStorage {

    /**
     * Puts the given value for the given key into the storage.
     */
    fun put(key: String, value: ByteArray)

    /**
     * Removes the given key from storage.
     */
    fun delete(key: String)
}
