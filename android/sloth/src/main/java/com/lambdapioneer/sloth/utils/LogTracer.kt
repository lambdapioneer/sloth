package com.lambdapioneer.sloth.utils

import android.util.Log

/**
 * A simple tracer that logs to the Android log. Used for the benchmarks tests and the output
 * is later extracted from the captured messages.
 */
class LogTracer(private val name: String) : Tracer {
    private val log = emptyList<Pair<String, String>>().toMutableList()

    override fun start() {
        addKeyValue("start", System.nanoTime().toString())
    }

    override fun step(name: String) {
        addKeyValue(name, System.nanoTime().toString())
    }

    override fun addKeyValue(key: String, value: String) {
        log.add(Pair(key, value))
    }

    override fun finish() {
        addKeyValue("finish", System.nanoTime().toString())

        val logString = log.joinToString(" ") { pair -> "${pair.first}=${pair.second}" }
        Log.i("LogTracer", "$name $logString")
    }

}
