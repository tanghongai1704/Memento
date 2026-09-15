package com.tangai.memento.feature.auth.domain

import com.tangai.memento.domain.model.User

interface AuthRepository {
    suspend fun login(account: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun syncCurrentUserProfile(): Result<Unit>
    suspend fun getCurrentUserProfile(): Result<User>
    suspend fun getCurrentUserInviteCode(): Result<String>
    suspend fun updateCurrentUserProfile(
        displayName: String,
        username: String,
        bio: String?
    ): Result<User>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    fun currentUserEmail(): String?
    fun logout()
    fun isUserLoggedIn(): Boolean
}
