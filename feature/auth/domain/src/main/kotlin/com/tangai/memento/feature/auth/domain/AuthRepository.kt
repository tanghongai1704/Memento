package com.tangai.memento.feature.auth.domain

import com.tangai.memento.domain.model.User

interface AuthRepository {
    suspend fun login(account: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<User>
    fun logout()
    fun isUserLoggedIn(): Boolean
}
