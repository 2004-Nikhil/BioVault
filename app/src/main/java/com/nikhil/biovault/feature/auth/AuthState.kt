package com.nikhil.biovault.feature.auth

sealed class AuthState {
    object Idle           : AuthState()
    object Authenticating : AuthState()
    object Authenticated  : AuthState()
    object Failed         : AuthState()
    data class LockedOut(val remainingMs: Long) : AuthState()
    data class Error(val message: String)       : AuthState()
}