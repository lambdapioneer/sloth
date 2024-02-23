package com.lambdapioneer.sloth.crypto

/**
 * This interface allows replacing the standard password hashing implementation with a custom one.
 *
 * See https://www.danielhugenroth.com/posts/2021_06_password_hashing_on_android/ for a introduction
 * to password hashing on Android.
 */
interface PwHash {

    /**
     * Creates a new salt to be used with the hash function. The length is implementation specific.
     */
    fun createSalt(): ByteArray

    /**
     * Derives a hash from the given salt and password.
     *
     * @param salt The salt to use for the hash function. See [createSalt].
     * @param password The password to use for the hash function.
     * @param outputLengthInBytes The length of the output hash in bytes.
     */
    fun deriveHash(salt: ByteArray, password: CharArray, outputLengthInBytes: Int): ByteArray
}
