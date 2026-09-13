package com.tangai.memento.feature.auth.data.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.tangai.memento.domain.model.User

fun DocumentSnapshot.toProfile(): User? {
    if (!exists()) return null
    val username = getString("username") ?: return null
    return User(id = id, username = username,
        displayName = getString("displayName") ?: username,
        usernameNormalized = getString("usernameNormalized")
            ?: com.tangai.memento.domain.model.normalizeUsername(username),
        avatarPath = getString("avatarPath"), bio = getString("bio"),
        createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0,
        updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: 0,
        schemaVersion = (getLong("schemaVersion") ?: 1).toInt())
}
