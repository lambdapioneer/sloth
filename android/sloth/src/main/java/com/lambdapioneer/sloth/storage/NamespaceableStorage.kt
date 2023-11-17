package com.lambdapioneer.sloth.storage

/**
 * A storage that allows to create sub-namespaces. For on-disk storage this would e.g. be a folder
 * and for in-memory storage this would be a prefix for the keys.
 */
interface NamespaceableStorage<out T> {

    /**
     * Creates a new instance of the storage with the given name as a sub-namespace of this
     * instance.
     */
    fun getOrCreateNamespace(name: String): T

    /**
     * Removes the namespace with the given name. Returns silently if the namespace does not exist.
     */
    fun deleteNamespace(name: String)
}
