package com.lambdapioneer.sloth.impl

/**
 * Cached secrets for the [HiddenSlothImpl] implementation. This can be used to speed up repeated
 * access to the ciphertext.
 */
data class HiddenSlothCachedSecrets(
    val iv: ByteArray,
    val tk: ByteArray,
    val tiv: ByteArray,
    val k: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HiddenSlothCachedSecrets

        if (!iv.contentEquals(other.iv)) return false
        if (!tk.contentEquals(other.tk)) return false
        if (!tiv.contentEquals(other.tiv)) return false
        if (!k.contentEquals(other.k)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + tk.contentHashCode()
        result = 31 * result + tiv.contentHashCode()
        result = 31 * result + k.contentHashCode()
        return result
    }
}
