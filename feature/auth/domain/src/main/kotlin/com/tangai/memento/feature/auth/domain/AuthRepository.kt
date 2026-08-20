package com.tangai.memento.feature.auth.domain

interface AuthRepository {
    suspend fun login(account: String, password: String): Result<Unit>
    suspend fun signup(username: String, account: String, password: String): Result<Unit>
}
