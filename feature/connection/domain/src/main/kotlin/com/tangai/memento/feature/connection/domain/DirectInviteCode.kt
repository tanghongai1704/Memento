package com.tangai.memento.feature.connection.domain

data class DirectInviteCode(
    val code: String,
    val expiresAtMillis: Long
)
