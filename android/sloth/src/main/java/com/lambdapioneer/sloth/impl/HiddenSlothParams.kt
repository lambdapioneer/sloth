package com.lambdapioneer.sloth.impl

/**
 * The HiddenSloth parameters as defined in the paper.
 */
data class HiddenSlothParams(
    internal val payloadMaxLength: Int,
    internal val longSlothParams: LongSlothParams = LongSlothParams(),
    internal val lambda: Int = SECURITY_PARAMETER_LAMBDA_DEFAULT,
) {
    companion object {
        const val SECURITY_PARAMETER_LAMBDA_DEFAULT = 128
    }
}
