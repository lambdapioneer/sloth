package com.lambdapioneer.sloth

/**
 * Abstract base class for all exceptions thrown by Sloth.
 */
abstract class SlothException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Thrown when the decryption of a storage fails. This can mean that the password is wrong,
 * that the storage has been tampered with, or that no data was ever encrypted.
 */
class SlothDecryptionFailed(
    message: String,
    cause: Throwable? = null,
) : SlothException(message, cause)

/**
 * Thrown when a key identifier is invalid.
 */
class SlothBadIdentifier(message: String) : SlothException(message)

/**
 * Thrown if any inconsistent state is detected.
 */
class SlothInconsistentState(message: String) : SlothException(message)

/**
 * Thrown when a key is not found in the storage.
 */
class SlothStorageKeyNotFound : SlothException("Key not found in storage")
