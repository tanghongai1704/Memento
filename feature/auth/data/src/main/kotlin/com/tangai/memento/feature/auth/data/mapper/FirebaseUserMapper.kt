package com.tangai.memento.feature.auth.data.mapper

import com.google.firebase.auth.FirebaseUser
import com.tangai.memento.domain.model.User

fun FirebaseUser.toDomainUser(): User {
    val safeEmail = email.orEmpty()
    val username = safeEmail.substringBefore("@").ifBlank { uid }
    return User(
        id = uid,
        username = username,
        email = safeEmail
    )
}
