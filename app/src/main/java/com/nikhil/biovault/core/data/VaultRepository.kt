package com.nikhil.biovault.core.data

import com.nikhil.biovault.core.model.Credential
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VaultRepository(private val store: EncryptedPrefsStore) {

    companion object {
        private const val PREFIX       = "credential_"
        private const val INDEX_KEY    = "credential_index"
        private val json               = Json { ignoreUnknownKeys = true }
    }

    // ── Index management ───────────────────────────────────────────────
    // We keep a comma-separated list of IDs so we can enumerate all entries
    // without scanning every key in SharedPreferences

    private fun getIndex(): MutableList<String> {
        val raw = store.getString(INDEX_KEY) ?: return mutableListOf()
        return raw.split(",").filter { it.isNotBlank() }.toMutableList()
    }

    private fun saveIndex(index: List<String>) {
        store.putString(INDEX_KEY, index.joinToString(","))
    }

    // ── CRUD ────────────────────────────────────────────────────────────

    fun getAll(): List<Credential> {
        val index = getIndex()
        return index.mapNotNull { id ->
            val raw = store.getString("$PREFIX$id") ?: return@mapNotNull null
            runCatching { json.decodeFromString<Credential>(raw) }.getOrNull()
        }.sortedByDescending { it.updatedAt }
    }

    fun getById(id: String): Credential? {
        val raw = store.getString("$PREFIX$id") ?: return null
        return runCatching { json.decodeFromString<Credential>(raw) }.getOrNull()
    }

    fun save(credential: Credential) {
        val encoded = json.encodeToString(credential)
        store.putString("$PREFIX${credential.id}", encoded)

        val index = getIndex()
        if (!index.contains(credential.id)) {
            index.add(credential.id)
            saveIndex(index)
        }
    }

    fun update(credential: Credential) {
        val updated = credential.copy(updatedAt = System.currentTimeMillis())
        store.putString("$PREFIX${credential.id}", json.encodeToString(updated))
    }

    fun delete(id: String) {
        store.remove("$PREFIX$id")
        val index = getIndex().also { it.remove(id) }
        saveIndex(index)
    }

    fun count(): Int = getIndex().size
}