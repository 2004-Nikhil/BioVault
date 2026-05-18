package com.nikhil.biovault.core.security

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Observes the app-level lifecycle.
 * ON_STOP  → starts a grace-period timer (default 15s)
 *            so brief interruptions (notification shade) don't lock
 * ON_START → cancels the timer if user returns in time
 *
 * After grace period expires → AppLockState.lock() fires
 * which triggers recomposition and shows AuthScreen.
 */

class AutoLockManager(
    private val gracePeriodMs: Long = 15_000L
) : DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private var lockRunnable: Runnable? = null
    private var backgroundTimestamp: Long = 0L

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun unregister() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        cancelPendingLock()
    }

    override fun onStop(owner: LifecycleOwner) {
        // 1. Record the exact time we left
        backgroundTimestamp = System.currentTimeMillis()

        // 2. Schedule a lock in case the process stays alive in the background
        cancelPendingLock()
        lockRunnable = Runnable {
            AppLockState.lock()
        }.also {
            handler.postDelayed(it, gracePeriodMs)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // 1. Check if the grace period has passed since we backgrounded
        val timeInBackground = System.currentTimeMillis() - backgroundTimestamp

        if (backgroundTimestamp != 0L && timeInBackground >= gracePeriodMs) {
            // User was gone too long, lock immediately
            AppLockState.lock()
        }

        // 2. Clean up the timer and timestamp
        cancelPendingLock()
        backgroundTimestamp = 0L
    }

    private fun cancelPendingLock() {
        lockRunnable?.let { handler.removeCallbacks(it) }
        lockRunnable = null
    }
}