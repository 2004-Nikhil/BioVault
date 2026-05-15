package com.nikhil.biovault.feature.vault

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.nikhil.biovault.core.data.EncryptedPrefsStore
import com.nikhil.biovault.core.data.VaultRepository
import com.nikhil.biovault.core.model.Credential

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(EncryptedPrefsStore(application))

    // SnapshotStateList drives recomposition automatically
    val credentials = mutableStateListOf<Credential>()

    private var _searchQuery = androidx.compose.runtime.mutableStateOf("")
    val searchQuery: androidx.compose.runtime.State<String> = _searchQuery

    init {
        loadAll()
    }

    // ── Data ops ────────────────────────────────────────────────────────

    private fun loadAll() {
        credentials.clear()
        credentials.addAll(repository.getAll())
    }

    fun addCredential(credential: Credential) {
        repository.save(credential)
        credentials.add(0, credential) // insert at top (newest first)
    }

    fun updateCredential(credential: Credential) {
        repository.update(credential)
        val index = credentials.indexOfFirst { it.id == credential.id }
        if (index != -1) credentials[index] = credential
    }

    fun deleteCredential(id: String) {
        repository.delete(id)
        credentials.removeAll { it.id == id }
    }

    fun getById(id: String): Credential? = repository.getById(id)

    // ── Search ──────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    val filteredCredentials: List<Credential>
        get() {
            val q = _searchQuery.value.trim().lowercase()
            if (q.isEmpty()) return credentials
            return credentials.filter {
                it.site.lowercase().contains(q) ||
                        it.username.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q)
            }
        }
}