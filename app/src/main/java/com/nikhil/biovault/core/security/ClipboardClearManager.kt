package com.nikhil.biovault.core.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ClipboardClearManager(private val context: Context) {

    companion object {
        private const val CLEAR_DELAY_MS  = 30_000L
        private const val TICK_INTERVAL   = 1_000L
    }

    private var timer: CountDownTimer? = null

    // Remaining seconds exposed so UI can show a countdown badge
    private val _secondsRemaining = MutableStateFlow(0L)
    val secondsRemaining: StateFlow<Long> = _secondsRemaining

    private val _isClearPending = MutableStateFlow(false)
    val isClearPending: StateFlow<Boolean> = _isClearPending

    /**
     * Call this immediately after copying a password.
     * Cancels any existing timer and starts a fresh 30s countdown.
     */
    fun scheduleClear(label: String = "password") {
        cancelPending()
        _isClearPending.value  = true
        _secondsRemaining.value = CLEAR_DELAY_MS / 1_000

        timer = object : CountDownTimer(CLEAR_DELAY_MS, TICK_INTERVAL) {

            override fun onTick(millisUntilFinished: Long) {
                _secondsRemaining.value = millisUntilFinished / 1_000
            }

            override fun onFinish() {
                clearNow()
            }
        }.start()
    }

    /** Immediately wipe the clipboard and cancel any pending timer. */
    fun clearNow() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Replace clipboard content with empty string
        // (setting to null is not supported on all API levels)
        cm.setPrimaryClip(ClipData.newPlainText("", ""))
        cancelPending()
    }

    fun cancelPending() {
        timer?.cancel()
        timer = null
        _isClearPending.value   = false
        _secondsRemaining.value = 0L
    }
}