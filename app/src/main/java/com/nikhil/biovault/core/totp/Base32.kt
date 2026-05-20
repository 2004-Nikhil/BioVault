package com.nikhil.biovault.core.totp

/**
 * RFC 4648 Base32 decoder.
 * TOTP secrets from Google Authenticator, Authy etc are always Base32.
 * No external library needed — pure Kotlin bit manipulation.
 */
object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(input: String): ByteArray {
        // Normalise — strip spaces, dashes, convert to uppercase
        val cleaned = input.uppercase()
            .replace(" ", "")
            .replace("-", "")
            .trimEnd('=') // strip padding

        if (cleaned.isEmpty()) return ByteArray(0)

        val outputLength = cleaned.length * 5 / 8
        val result       = ByteArray(outputLength)

        var buffer    = 0L
        var bitsLeft  = 0
        var index     = 0

        for (char in cleaned) {
            val value = ALPHABET.indexOf(char)
            require(value >= 0) { "Invalid Base32 character: $char" }

            buffer   = (buffer shl 5) or value.toLong()
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result[index++] = (buffer shr bitsLeft).toByte()
                buffer = buffer and ((1L shl bitsLeft) - 1L)
            }
        }

        return result
    }

    /**
     * Validate a Base32 secret without throwing.
     * Used to show inline error in the AddEdit form.
     */
    fun isValid(input: String): Boolean {
        if (input.isBlank()) return true // empty = optional field, not an error
        return try {
            val cleaned = input.uppercase().replace(" ", "").replace("-", "").trimEnd('=')
            cleaned.all { it in ALPHABET }
                    && cleaned.isNotEmpty()
                    && decode(input).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}