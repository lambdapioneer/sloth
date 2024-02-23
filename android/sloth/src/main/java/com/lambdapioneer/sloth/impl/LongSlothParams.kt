package com.lambdapioneer.sloth.impl

import com.lambdapioneer.sloth.utils.ceilToNextKiB

/**
 * The LongSloth parameters as defined in the paper.
 *
 * The [l] parameter is rounded up to the next KiB. See [SlothLib.determineParameter] as a practical
 * way to determine a suitable L parameter.
 *
 * The [lambda] parameter is the overall security parameter and usually set to 128.
 */
class LongSlothParams(
    l: Int = SECURITY_PARAMETER_L_DEFAULT,
    internal val lambda: Int = SECURITY_PARAMETER_LAMBDA_DEFAULT
) {
    internal val l = ceilToNextKiB(l)

    companion object {
        const val SECURITY_PARAMETER_L_DEFAULT = 100 * 1024
        const val SECURITY_PARAMETER_LAMBDA_DEFAULT = 128
    }
}
