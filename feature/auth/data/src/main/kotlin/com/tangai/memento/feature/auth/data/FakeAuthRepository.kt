package com.tangai.memento.feature.auth.data

import com.tangai.memento.feature.auth.domain.AuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class FakeAuthRepository @Inject constructor() : AuthRepository {
    override suspend fun login(account: String, password: String): Result<Unit> {
        delay(1000)
        return Result.success(Unit)
    }

    override suspend fun signup(username: String, account: String, password: String): Result<Unit> {
        delay(1000)
        return Result.success(Unit)
    }
}
