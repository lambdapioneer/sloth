package com.lambdapioneer.sloth.storage

/**
 * The user-facing storage interface. This combines the [ReadableStorage], [WriteableStorage]
 * and [NamespaceableStorage] interface. Inside the library we differentiate between these
 * interfaces to make it clear which code paths write and which only read.
 */
interface SlothStorage : ReadableStorage, WriteableStorage, NamespaceableStorage<SlothStorage>
