package com.nikhil.biovault.core.generator

import kotlin.math.log2
import kotlin.math.pow
import java.security.SecureRandom

data class GeneratorConfig(
    val length: Int = 16,
    val useUppercase: Boolean = true,
    val useLowercase: Boolean = true,
    val useDigits: Boolean = true,
    val useSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false  // excludes 0,O,l,1,I
)

data class GeneratedPassword(
    val password: String,
    val entropyBits: Double,
    val charsetSize: Int
)

object PasswordGeneratorEngine {

    // Ambiguous characters that look similar across fonts
    private const val AMBIGUOUS = "0O1lI"

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS    = "0123456789"
    private const val SYMBOLS   = "!@#\$%^&*()-_=+[]{}|;:,.<>?"

    private val secureRandom = SecureRandom()

    fun generate(config: GeneratorConfig): GeneratedPassword {
        val charset = buildCharset(config)

        require(charset.isNotEmpty()) {
            "At least one character set must be selected"
        }

        // Guarantee at least one character from each enabled set
        val guaranteed = buildGuaranteedChars(config)

        // Fill remaining slots randomly
        val remaining = (guaranteed.size until config.length).map {
            charset[secureRandom.nextInt(charset.length)]
        }

        // Shuffle so guaranteed chars aren't always at the front
        val passwordChars = (guaranteed + remaining).toMutableList()
        passwordChars.shuffle(secureRandom)

        val password = passwordChars.joinToString("")
        val entropy  = calculateEntropy(charset.length, config.length)

        return GeneratedPassword(
            password    = password,
            entropyBits = entropy,
            charsetSize = charset.length
        )
    }

    fun buildCharset(config: GeneratorConfig): String {
        val sb = StringBuilder()
        if (config.useUppercase) sb.append(UPPERCASE)
        if (config.useLowercase) sb.append(LOWERCASE)
        if (config.useDigits)    sb.append(DIGITS)
        if (config.useSymbols)   sb.append(SYMBOLS)

        return if (config.excludeAmbiguous) {
            sb.filter { it !in AMBIGUOUS }.toString()
        } else {
            sb.toString()
        }
    }

    private fun buildGuaranteedChars(config: GeneratorConfig): List<Char> {
        val guaranteed = mutableListOf<Char>()
        fun pickFrom(pool: String) {
            val filtered = if (config.excludeAmbiguous)
                pool.filter { it !in AMBIGUOUS } else pool
            if (filtered.isNotEmpty())
                guaranteed.add(filtered[secureRandom.nextInt(filtered.length)])
        }
        if (config.useUppercase) pickFrom(UPPERCASE)
        if (config.useLowercase) pickFrom(LOWERCASE)
        if (config.useDigits)    pickFrom(DIGITS)
        if (config.useSymbols)   pickFrom(SYMBOLS)
        return guaranteed
    }

    // Shannon entropy: log2(charsetSize) * length
    fun calculateEntropy(charsetSize: Int, length: Int): Double {
        if (charsetSize == 0) return 0.0
        return log2(charsetSize.toDouble()) * length
    }

    fun entropyLabel(bits: Double): String = when {
        bits < 28  -> "Terrible"
        bits < 36  -> "Weak"
        bits < 60  -> "Reasonable"
        bits < 80  -> "Strong"
        bits < 128 -> "Very Strong"
        else       -> "Overkill"
    }

    fun entropyDescription(bits: Double): String =
        "%.1f bits — %s".format(bits, entropyLabel(bits))
}