package com.lambdapioneer.sloth.app

import android.app.Application
import android.widget.Toast
import com.lambdapioneer.sloth.SlothLib
import com.lambdapioneer.sloth.pwhash_libsodium.LibSodiumArgon2PwHash

class SampleApplication : Application() {

    private val slothLib = SlothLib(pwHash = LibSodiumArgon2PwHash())

    override fun onCreate() {
        super.onCreate()
        try {
            slothLib.init(this)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                e.message,
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    fun getSlothLib(): SlothLib {
        return slothLib
    }
}
