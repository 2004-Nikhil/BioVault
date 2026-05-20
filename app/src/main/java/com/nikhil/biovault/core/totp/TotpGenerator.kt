package com.nikhil.biovault.core.totp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * RFC 6238 TOTP implementation.
 * Uses HMAC-SHA1 per the spec (same as Google Authenticator).
 */
object TotpGenerator {

    private const val TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS       = 6
    private const val ALGORITHM         = "HmacSHA1"

    data class TotpCode(
        val code: String,           // zero-padded 6-digit string e.g. "047382"
        val secondsRemaining: Int,  // seconds until next rotation
        val progress: Float         // 1.0 → 0.0 countdown for UI ring
    )

    /**
     * Generate the current TOTP code for a given Base32 secret.
     * Returns null if the secret is invalid.
     */
    fun generate(secret: String): TotpCode? {
        return try {
            val keyBytes  = Base32.decode(secret)
            val timeStep  = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS
            val remaining = (TIME_STEP_SECONDS - (System.currentTimeMillis() / 1000L % TIME_STEP_SECONDS)).toInt()
            val progress  = remaining / TIME_STEP_SECONDS.toFloat()

            val code = computeHotp(keyBytes, timeStep)
            TotpCode(
                code             = code,
                secondsRemaining = remaining,
                progress         = progress
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * HOTP(K, C) = Truncate(HMAC-SHA1(K, C))
     * RFC 4226 Section 5
     */
    private fun computeHotp(key: ByteArray, counter: Long): String {
        // Step 1 — HMAC-SHA1
        val mac       = Mac.getInstance(ALGORITHM)
        val keySpec   = SecretKeySpec(key, ALGORITHM)
        mac.init(keySpec)

        // Counter as big-endian 8-byte array
        val counterBytes = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (c and 0xFF).toByte()
            c = c shr 8
        }
        val hash = mac.doFinal(counterBytes)

        // Step 2 — Dynamic truncation
        val offset = hash.last().toInt() and 0x0F
        val binary = ((hash[offset].toInt()     and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8)  or
                (hash[offset + 3].toInt() and 0xFF)

        // Step 3 — Modulo
        val otp = binary % 10.0.pow(CODE_DIGITS).toInt()
        return otp.toString().padStart(CODE_DIGITS, '0')
    }
}