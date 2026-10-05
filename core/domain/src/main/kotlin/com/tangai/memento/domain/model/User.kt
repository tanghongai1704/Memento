package com.tangai.memento.domain.model

import java.util.Locale

fun normalizeUsername(value: String): String = value.filterNot { it.isWhitespace() }.lowercase(Locale.ROOT)

data class User(
    val id: String,
    val username: String,
    // Auth-only data; never persisted in the public Firestore profile.
    val email: String = "",
    val displayName: String = username,
    val usernameNormalized: String = normalizeUsername(username),
    val avatarPath: String? = null,
    val bio: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val schemaVersion: Int = 1
)
