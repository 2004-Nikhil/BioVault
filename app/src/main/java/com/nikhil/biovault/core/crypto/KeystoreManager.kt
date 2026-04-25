package com.nikhil.biovault.core.crypto

import javax.crypto.SecretKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

object KeystoreManager {
    private const val KEY_ALIAS = "vault_master_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

    fun getOrCreateSecretKey(): SecretKey {
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }
        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        ).also {
            it.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)          // requires biometric
                    .setInvalidatedByBiometricEnrollment(true)    // key invalidated on new fingerprint
                    .build()
            )
        }.generateKey()
    }

    fun isKeyValid(): Boolean = runCatching {
        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return false
        Cipher.getInstance("AES/GCM/NoPadding").init(Cipher.ENCRYPT_MODE, key)
        true
    }.getOrElse { false }
}