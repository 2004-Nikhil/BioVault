package com.nikhil.biovault.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Credential(
    val id: String = UUID.randomUUID().toString(),
    val site: String,
    val username: String,
    val password: String,
    val totpSecret: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)