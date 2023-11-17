package com.lambdapioneer.sloth.storage

import com.lambdapioneer.sloth.SlothStorageKeyNotFound

interface ReadableStorage {

    /**
     * Returns the value for the given key. If the key is not found, a [SlothStorageKeyNotFound]
     * exception is thrown.
     */
    @Throws(SlothStorageKeyNotFound::class)
    fun get(key: String): ByteArray
}
