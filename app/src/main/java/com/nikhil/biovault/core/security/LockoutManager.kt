package com.nikhil.biovault.core.security

import com.nikhil.biovault.core.data.EncryptedPrefsStore

class LockoutManager(private val store: EncryptedPrefsStore) {

    companion object {
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL   = "lockout_until_ms"
        private const val MAX_ATTEMPTS        = 5
        private const val BASE_DELAY_MS       = 30_000L  // 30 seconds
    }

    fun recordFailedAttempt() {
        val attempts = getFailedAttempts() + 1
        store.putInt(KEY_FAILED_ATTEMPTS, attempts)

        if (attempts >= MAX_ATTEMPTS) {
            // Exponential backoff: 30s, 60s, 120s, 240s …
            val multiplier = Math.pow(2.0, (attempts - MAX_ATTEMPTS).toDouble()).toLong()
            val lockDuration = BASE_DELAY_MS * multiplier
            val lockUntil = System.currentTimeMillis() + lockDuration
            store.putString(KEY_LOCKOUT_UNTIL, lockUntil.toString())
        }
    }

    fun clearFailedAttempts() {
        store.remove(KEY_FAILED_ATTEMPTS)
        store.remove(KEY_LOCKOUT_UNTIL)
    }

    fun getFailedAttempts(): Int = store.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun isLockedOut(): Boolean {
        val lockUntil = store.getString(KEY_LOCKOUT_UNTIL)?.toLongOrNull() ?: return false
        return System.currentTimeMillis() < lockUntil
    }

    fun lockoutRemainingMs(): Long {
        val lockUntil = store.getString(KEY_LOCKOUT_UNTIL)?.toLongOrNull() ?: return 0L
        return maxOf(0L, lockUntil - System.currentTimeMillis())
    }

    fun remainingAttemptsBeforeLockout(): Int {
        return maxOf(0, MAX_ATTEMPTS - getFailedAttempts())
    }
}