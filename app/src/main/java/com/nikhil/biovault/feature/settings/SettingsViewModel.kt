package com.nikhil.biovault.feature.settings


import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nikhil.biovault.core.data.EncryptedPrefsStore
import com.nikhil.biovault.core.data.SettingsStore
import com.nikhil.biovault.core.data.VaultRepository

class SettingsViewModelFactory(
    private val settingsStore: SettingsStore,
    private val repository: VaultRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsStore, repository) as T
    }
}

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    var screenCaptureBlocked by mutableStateOf(settingsStore.isScreenCaptureBlocked())
        private set

    var selectedTimeoutLabel by mutableStateOf(settingsStore.getAutoLockLabel())
        private set

    var showWipeConfirm by mutableStateOf(false)

    var credentialCount by mutableStateOf(vaultRepository.count())
        private set

    fun setAutoLockTimeout(label: String) {
        val ms = SettingsStore.TIMEOUT_OPTIONS[label] ?: SettingsStore.DEFAULT_TIMEOUT
        settingsStore.setAutoLockTimeout(ms)
        selectedTimeoutLabel = label
    }

    fun toggleScreenCapture() {
        val newValue = !screenCaptureBlocked
        settingsStore.setScreenCaptureBlocked(newValue) // This triggers the Flow in MainActivity!
        screenCaptureBlocked = newValue
    }

    fun wipeVault() {
        // Delete every credential one by one via repository
        val all = vaultRepository.getAll()
        all.forEach { vaultRepository.delete(it.id) }
        credentialCount = 0
        showWipeConfirm = false
    }
}