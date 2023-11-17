package com.lambdapioneer.sloth.testing

import android.os.Build


fun getDeviceName(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
}

/**
 * Looks up the parameter for the given device based on a look-up table
 */
fun getParameterForDevice(
    configName: String,
    deviceName: String = getDeviceName(),
): Int {
    // return simplest configuration when using the emulator
    if (deviceName == "unknown Android SDK built for x86_64") {
        return 1024
    }

    // otherwise, look it up in the generated table
    return when (configName) {
        "small" -> CONFIG_TABLE_SMALL[deviceName]!!
        "large" -> CONFIG_TABLE_LARGE[deviceName]!!
        else -> TODO("unsupported config name")
    }
}
