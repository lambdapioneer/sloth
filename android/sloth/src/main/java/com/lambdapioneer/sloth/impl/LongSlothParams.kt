package com.lambdapioneer.sloth.impl

import com.lambdapioneer.sloth.utils.ceilToNextKiB

/**
 * The LongSloth parameters as defined in the paper. The L parameter is rounded up to the next KiB.
 *
 * See [SlothLib.determineParameter] as a practical way to determine a suitable L parameter.
 */
class LongSlothParams(l: Int = SECURITY_PARAMETER_L_DEFAULT) {
    val l = ceilToNextKiB(l)

    companion object {
        const val SECURITY_PARAMETER_L_DEFAULT = 100 * 1024
    }
}
