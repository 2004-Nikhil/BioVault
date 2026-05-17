package com.nikhil.biovault.core.generator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.nikhil.biovault.core.generator.GeneratedPassword
import com.nikhil.biovault.core.generator.GeneratorConfig
import com.nikhil.biovault.core.generator.PasswordGeneratorEngine

class GeneratorViewModel : ViewModel() {

    var config by mutableStateOf(GeneratorConfig())
        private set

    var result by mutableStateOf<GeneratedPassword?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        regenerate() // generate one immediately on open
    }

    fun regenerate() {
        errorMessage = null
        val charset = PasswordGeneratorEngine.buildCharset(config)
        if (charset.isEmpty()) {
            errorMessage = "Select at least one character set"
            result = null
            return
        }
        result = PasswordGeneratorEngine.generate(config)
    }

    fun setLength(length: Int) {
        config = config.copy(length = length)
        regenerate()
    }

    fun toggleUppercase() {
        config = config.copy(useUppercase = !config.useUppercase)
        regenerate()
    }

    fun toggleLowercase() {
        config = config.copy(useLowercase = !config.useLowercase)
        regenerate()
    }

    fun toggleDigits() {
        config = config.copy(useDigits = !config.useDigits)
        regenerate()
    }

    fun toggleSymbols() {
        config = config.copy(useSymbols = !config.useSymbols)
        regenerate()
    }

    fun toggleExcludeAmbiguous() {
        config = config.copy(excludeAmbiguous = !config.excludeAmbiguous)
        regenerate()
    }
}