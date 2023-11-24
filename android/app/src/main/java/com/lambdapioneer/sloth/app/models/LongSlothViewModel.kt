package com.lambdapioneer.sloth.app.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.lambdapioneer.sloth.SlothLib
import com.lambdapioneer.sloth.app.SampleApplication
import com.lambdapioneer.sloth.impl.LongSlothParams
import com.lambdapioneer.sloth.storage.OnDiskStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LongSlothViewModel(
    slothLib: SlothLib,
    private val storage: OnDiskStorage,
) : ViewModel() {
    val key = MutableStateFlow<ByteArray?>(null)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private val longSloth = slothLib.getLongSlothInstance(
        identifier = "test",
        params = LongSlothParams(l = 100_000)
    )

    fun generateKey(password: String) {
        runLongTaskInBackground {
            key.value = longSloth.createNewKey(pw = password, storage = storage)
        }
    }

    fun deriveKey(password: String) {
        runLongTaskInBackground {
            key.value = longSloth.deriveForExistingKey(pw = password, storage = storage)
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
                return LongSlothViewModel(
                    slothLib = application.getSlothLib(),
                    storage = OnDiskStorage(application)
                ) as T
            }
        }
    }
}
