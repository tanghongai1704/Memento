package com.tangai.memento.domain.model

data class User(
    val id: String,
    val username: String,
    val email: String = ""
)
