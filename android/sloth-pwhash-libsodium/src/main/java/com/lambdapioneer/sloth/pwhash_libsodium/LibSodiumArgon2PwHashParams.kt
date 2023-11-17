package com.lambdapioneer.sloth.pwhash_libsodium

import com.sun.jna.NativeLong

enum class LibSodiumArgon2PwHashParams(
    internal val opsLimit: Long,
    internal val memLimit: NativeLong,
) {
    /**
     * See https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
     */
    OWASP(
        2L,
        NativeLong(19 * 1024 * 1024) // // 19 MiB
    ),
    INTERACTIVE(
        com.goterl.lazysodium.interfaces.PwHash.OPSLIMIT_INTERACTIVE,
        com.goterl.lazysodium.interfaces.PwHash.MEMLIMIT_INTERACTIVE,
    ),
    MODERATE(
        com.goterl.lazysodium.interfaces.PwHash.OPSLIMIT_MODERATE,
        com.goterl.lazysodium.interfaces.PwHash.MEMLIMIT_MODERATE,
    ),
}
