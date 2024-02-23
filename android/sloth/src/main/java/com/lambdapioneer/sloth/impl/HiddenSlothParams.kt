package com.lambdapioneer.sloth.impl

/**
 * The HiddenSloth parameters as defined in the paper.
 */
data class HiddenSlothParams(
    internal val payloadMaxLength: Int,
    internal val longSlothParams: LongSlothParams = LongSlothParams(),
)
