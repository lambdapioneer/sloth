package com.lambdapioneer.sloth.utils

/**
 * A tracer is used to collect performance data during the execution of Sloth. It is used to
 * measure the time spent in each step of the algorithm.
 */
interface Tracer {
    fun start()
    fun step(name: String)
    fun addKeyValue(key: String, value: String)
    fun finish()
}

