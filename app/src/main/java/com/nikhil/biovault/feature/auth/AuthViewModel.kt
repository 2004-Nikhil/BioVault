package com.nikhil.biovault.feature.auth

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.biovault.core.data.EncryptedPrefsStore
import com.nikhil.biovault.core.security.AuthResult
import com.nikhil.biovault.core.security.BiometricAuthManager
import com.nikhil.biovault.core.security.LockoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val store          = EncryptedPrefsStore(application)
    private val lockoutManager = LockoutManager(store)
    private val biometricMgr   = BiometricAuthManager(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var countdownJob: Job? = null

    fun checkInitialState() {
        _authState.value = AuthState.Idle

        if (lockoutManager.isLockedOut()) {
            startLockoutCountdown()
        }
    }

    fun authenticate(activity: FragmentActivity) {
        if (lockoutManager.isLockedOut()) {
            startLockoutCountdown()
            return
        }

        _authState.value = AuthState.Authenticating

        biometricMgr.authenticate(activity) { result ->
            when (result) {
                is AuthResult.Success -> {
                    lockoutManager.clearFailedAttempts()
                    _authState.value = AuthState.Authenticated
                }
                is AuthResult.Failed -> {
                    lockoutManager.recordFailedAttempt()
                    if (lockoutManager.isLockedOut()) {
                        startLockoutCountdown()
                    } else {
                        _authState.value = AuthState.Failed
                    }
                }
                is AuthResult.Cancelled -> {
                    _authState.value = AuthState.Idle
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    private fun startLockoutCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (lockoutManager.isLockedOut()) {
                _authState.value = AuthState.LockedOut(lockoutManager.lockoutRemainingMs())
                delay(1_000)
            }
            _authState.value = AuthState.Idle
        }
    }

    fun isBiometricAvailable(): Boolean = biometricMgr.isBiometricAvailable()

    fun remainingAttempts(): Int = lockoutManager.remainingAttemptsBeforeLockout()

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    // Add this to allow manual resetting
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}