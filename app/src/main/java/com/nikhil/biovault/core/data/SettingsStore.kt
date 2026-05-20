package com.nikhil.biovault.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
/**
 * All user-configurable settings stored encrypted.
 * Kept separate from VaultRepository so concerns are isolated.
 */
class SettingsStore(private val store: EncryptedPrefsStore) {

    // Create a Flow that holds the current state of screen capture blocking
    private val _isScreenCaptureBlocked = MutableStateFlow(isScreenCaptureBlocked())
    val isScreenCaptureBlockedFlow: StateFlow<Boolean> = _isScreenCaptureBlocked

    companion object {
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout_ms"
        private const val KEY_SCREEN_CAPTURE    = "screen_capture_blocked"

        // Preset timeout values in milliseconds
        val TIMEOUT_OPTIONS = linkedMapOf(
            "15 seconds" to 15_000L,
            "1 minute"   to 60_000L,
            "5 minutes"  to 300_000L,
            "15 minutes" to 900_000L,
            "Never"      to Long.MAX_VALUE
        )
        val DEFAULT_TIMEOUT = 15_000L
    }

    fun getAutoLockTimeout(): Long {
        val stored = store.getString(KEY_AUTO_LOCK_TIMEOUT)?.toLongOrNull()
        return stored ?: DEFAULT_TIMEOUT
    }

    fun setAutoLockTimeout(ms: Long) {
        store.putString(KEY_AUTO_LOCK_TIMEOUT, ms.toString())
    }

    fun getAutoLockLabel(): String {
        val current = getAutoLockTimeout()
        return TIMEOUT_OPTIONS.entries
            .firstOrNull { it.value == current }?.key ?: "15 seconds"
    }

    fun isScreenCaptureBlocked(): Boolean {
        return store.getString(KEY_SCREEN_CAPTURE)?.toBooleanStrictOrNull() ?: true
    }

    fun setScreenCaptureBlocked(blocked: Boolean) {
        store.putString(KEY_SCREEN_CAPTURE, blocked.toString())
        // Update the flow so listeners (like MainActivity) react immediately
        _isScreenCaptureBlocked.value = blocked
    }
}