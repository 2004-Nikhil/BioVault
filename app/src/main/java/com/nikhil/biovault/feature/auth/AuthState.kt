package com.nikhil.biovault.feature.auth

sealed class AuthState {
    object Idle           : AuthState()
    object Authenticating : AuthState()
    object Authenticated  : AuthState()
    object Failed         : AuthState()
    object BiometricUnavailable                : AuthState()

    // Lockout with live countdown
    data class LockedOut(val remainingMs: Long) : AuthState()

    // Key was permanently invalidated — new biometric enrolled
    // User must acknowledge before vault can be accessed again
    object KeyInvalidated                      : AuthState()

    // After user acknowledges invalidation, key is deleted and
    // a fresh one generated — show a one-time re-enroll prompt
    object ReadyForReEnrollment                : AuthState()

    data class Error(val message: String)       : AuthState()
}