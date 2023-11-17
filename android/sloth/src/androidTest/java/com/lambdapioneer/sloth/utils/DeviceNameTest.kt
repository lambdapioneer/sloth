package com.lambdapioneer.sloth.utils

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lambdapioneer.sloth.testing.getDeviceName
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "DeviceNameTest"

@RunWith(AndroidJUnit4::class)
class DeviceNameTest {

    @Test
    fun testDeviceName() {
        Log.i(TAG, getDeviceName())
    }
}
