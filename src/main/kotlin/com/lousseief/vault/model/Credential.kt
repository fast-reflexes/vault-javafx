package com.lousseief.vault.model

import java.time.Instant

// actual underlying data classes with the full information
data class Credential(
    val identities: List<String>,
    val password: String,
    val created: Instant = Instant.now(),
    val lastUpdated: Instant = Instant.now()
)
