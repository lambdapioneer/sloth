package com.lambdapioneer.sloth.utils

/**
 * Rounds up to the next multiple of 1024.
 */
internal fun ceilToNextKiB(x: Int): Int {
    val remainder = x % 1024
    return if (remainder == 0) x
    else (x + 1024 - remainder)
}

/**
 * Computes ceil(a/b) for positive integer values a, b.
 */
internal fun ceilOfIntegerDivision(a: Int, b: Int): Int {
    check(a > 0)
    check(b > 0)
    return if (a % b == 0) {
        a / b
    } else {
        a / b + 1
    }
}
