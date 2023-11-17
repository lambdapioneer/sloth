package com.lambdapioneer.sloth.crypto

import androidx.annotation.RequiresApi

enum class Pbkdf2PwHashParams(val algorithm: String, val iterationCount: Int) {
    /**
     * Default choice with low iteration count as the LongSloth brute-force resistance relies on
     * the secure element. Do not use this configuration without Sloth.
     */
    INTERACTIVE(algorithm = "PBKDF2WithHmacSha1", iterationCount = 1),

    /**
     * See https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2
     */
    OWASP(algorithm = "PBKDF2WithHmacSha1", iterationCount = 1_300_000),

    /**
     * See https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2
     *
     * Note that PBKDF2 with SHA256 is only supported from API 26+
     */
    @RequiresApi(26)
    OWASP_SHA256(algorithm = "PBKDF2WithHmacSha256", iterationCount = 1_300_000),
}
