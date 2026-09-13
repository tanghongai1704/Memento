package com.tangai.memento.feature.auth.data.mapper

import com.google.firebase.auth.FirebaseUser
import com.tangai.memento.domain.model.User

fun FirebaseUser.toDomainUser(): User {
    val safeEmail = email.orEmpty()
    val emailPrefix = safeEmail.substringBefore("@")
    val sanitized = emailPrefix.filter {
        it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '.' || it == '_'
    }
        .take(30)
        .dropWhile { it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' }
    val username = sanitized.takeIf { it.length >= 2 }
        ?: "user${uid.filter { it.isLetterOrDigit() }.take(12)}"
    return User(
        id = uid,
        username = username,
        email = safeEmail
    )
}
