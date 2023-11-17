package com.lambdapioneer.sloth.bench


const val SQRT_10 = 3.1622776601683795

/**
 * This should "theoretically" not be necessary for stream ciphers, however, Samsung begs to
 * differ and otherwise fails with `IllegalBlockSizeException`.
 */
internal fun ceilToNextBlockSize(x: Int, blockSize: Int = 16): Long {
    val remainder = x % blockSize
    return if (remainder == 0) x.toLong()
    else (x + blockSize - remainder).toLong()
}
