package com.nikhil.biovault.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

sealed class KeyStatus {
    object Valid       : KeyStatus()
    object Missing     : KeyStatus()  // first launch or manually deleted
    object Invalidated : KeyStatus()  // new biometric enrolled / all removed
}

object KeystoreManager {
    private const val KEY_ALIAS = "vault_master_key"
    private val keyStore: KeyStore =
        KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

    fun getOrCreateSecretKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing != null) return existing as SecretKey

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        ).also { keyGen ->
            keyGen.init(
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

    /**
     * Returns KeyStatus so callers can distinguish between
     * a missing key (first run) and an invalidated key (new fingerprint enrolled).
     * These require different UX responses.
     */
    fun checkKeyStatus(): KeyStatus {
        return try {
            val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
                ?: return KeyStatus.Missing

            // Attempt to init a cipher — this is what triggers invalidation detection
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            KeyStatus.Valid

        } catch (e: KeyPermanentlyInvalidatedException) {
            // Android threw this — biometric enrollment changed
            KeyStatus.Invalidated

        } catch (e: Exception) {
            // Any other error treat as invalidated to be safe
            KeyStatus.Invalidated
        }
    }

    /**
     * Delete the invalidated key so a fresh one can be generated.
     * Called after user acknowledges the re-auth flow.
     */
    fun deleteKey() {
        try {
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: KeyStoreException) {
            // Already gone — safe to ignore
        }
    }

    fun isKeyValid(): Boolean = checkKeyStatus() == KeyStatus.Valid
}