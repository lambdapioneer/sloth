package com.lambdapioneer.sloth.secureelement

import com.lambdapioneer.sloth.utils.encodeAsHex

/**
 * A key handle is a unique identifier for a key stored in the secure element. Note that these
 * are shared between all libraries within the same application. Therefore, we need to make sure
 * to prepend a unique prefix to the key handle to avoid collisions.
 */
data class KeyHandle(val handleString: String) {
    constructor(handleBytes: ByteArray) : this(handleBytes.encodeAsHex())
}
