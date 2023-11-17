package com.lambdapioneer.sloth.utils

/**
 * A tracer that does nothing. Used as the default.
 */
class NoopTracer : Tracer {
    override fun start() {}
    override fun step(name: String) {}
    override fun addKeyValue(key: String, value: String) {}
    override fun finish() {}
}
