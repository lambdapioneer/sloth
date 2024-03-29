package com.lambdapioneer.sloth.app.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.lambdapioneer.sloth.SlothLib
import com.lambdapioneer.sloth.app.SampleApplication
import com.lambdapioneer.sloth.impl.HiddenSlothParams
import com.lambdapioneer.sloth.impl.LongSlothParams
import com.lambdapioneer.sloth.storage.OnDiskStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class HiddenSlothViewModel(
    slothLib: SlothLib,
    storage: OnDiskStorage,
) : ViewModel() {
    val output = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private val hiddenSloth = slothLib.getHiddenSlothInstance(
        identifier = "test",
        storage = storage,
        params = HiddenSlothParams(
            payloadMaxLength = 32 * 1024,
            longSlothParams = LongSlothParams(l = 10_000)
        )
    )

    fun ensure() {
        runLongTaskInBackground {
            hiddenSloth.onAppStart()
        }
    }

    fun ratchet() {
        runLongTaskInBackground {
            hiddenSloth.ratchet()
        }
    }

    fun store(password: String, content: String, useCachedSecrets: Boolean) {
        runLongTaskInBackground {
            val data = content.encodeToByteArray()
            val passwordCharArray = password.toCharArray()
            if (useCachedSecrets) {
                val cachedSecrets = hiddenSloth.computeCachedSecrets(pw = passwordCharArray)
                hiddenSloth.encryptToStorageWithCachedSecrets(
                    cachedSecrets = cachedSecrets,
                    data = data
                )
            } else {
                hiddenSloth.encryptToStorage(
                    pw = passwordCharArray,
                    data = data,
                )
            }
        }
    }

    fun load(password: String, useCachedSecrets: Boolean) {
        runLongTaskInBackground {
            val passwordCharArray = password.toCharArray()
            val contentBytes = if (useCachedSecrets) {
                val cachedSecrets = hiddenSloth.computeCachedSecrets(pw = passwordCharArray)
                hiddenSloth.decryptFromStorageWithCachedSecrets(cachedSecrets)
            } else {
                hiddenSloth.decryptFromStorage(pw = passwordCharArray)
            }
            this.output.value = contentBytes.decodeToString()
        }
    }

    private fun runLongTaskInBackground(block: () -> Unit) {
        busy.value = true
        viewModelScope.launch {
            launch(Dispatchers.Default) {
                try {
                    block()
                    error.value = null
                } catch (e: Exception) {
                    e.printStackTrace()
                    error.value = e.toString()
                }
                busy.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as SampleApplication
                return HiddenSlothViewModel(
                    slothLib = application.getSlothLib(),
                    storage = OnDiskStorage(application)
                ) as T
            }
        }
    }
}
