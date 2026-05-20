package com.nikhil.biovault.core.totp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Wraps TotpGenerator in a coroutine ticker.
 * Emit a fresh TotpCode every second so the UI ring animates smoothly.
 * Call start() when the screen is visible, stop() when it leaves composition.
 */
class TotpManager {

    private val _totpCode = MutableStateFlow<TotpGenerator.TotpCode?>(null)
    val totpCode: StateFlow<TotpGenerator.TotpCode?> = _totpCode

    private var tickerJob: Job? = null

    fun start(secret: String, scope: CoroutineScope) {
        if (secret.isBlank()) return
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                _totpCode.value = TotpGenerator.generate(secret)
                delay(1_000)
            }
        }
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob  = null
        _totpCode.value = null
    }
}