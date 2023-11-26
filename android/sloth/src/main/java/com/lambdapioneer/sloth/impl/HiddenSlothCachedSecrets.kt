package com.lambdapioneer.sloth.impl

/**
 * Cached secrets for the [HiddenSlothImpl] implementation. This can be used to speed up repeated
 * access to the ciphertext.
 */
data class HiddenSlothCachedSecrets(
    val k: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HiddenSlothCachedSecrets

        return !k.contentEquals(other.k)
    }

    override fun hashCode(): Int {
        return k.contentHashCode()
    }

}
