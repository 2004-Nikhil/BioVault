package com.nikhil.biovault.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton in-memory lock state.
 * When the app backgrounds, isLocked flips to true and
 * AuthScreen is shown again on resume.
 * Lives only in memory — never persisted — so process kill
 * always forces re-authentication.
 */
object AppLockState {

    private val _isLocked = MutableStateFlow(true) // always locked on cold start
    val isLocked: StateFlow<Boolean> = _isLocked

    fun lock() {
        _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun isCurrentlyLocked(): Boolean = _isLocked.value
}