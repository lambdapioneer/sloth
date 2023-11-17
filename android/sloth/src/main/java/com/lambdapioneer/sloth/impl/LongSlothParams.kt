package com.lambdapioneer.sloth.impl

import com.lambdapioneer.sloth.utils.ceilToNextKiB

/**
 * The LongSloth parameters as defined in the paper.
 */
class LongSlothParams(l: Int = SECURITY_PARAMETER_L_DEFAULT) {
    val l = ceilToNextKiB(l)

    companion object {
        const val SECURITY_PARAMETER_L_DEFAULT = 10 * 1024
    }
}
