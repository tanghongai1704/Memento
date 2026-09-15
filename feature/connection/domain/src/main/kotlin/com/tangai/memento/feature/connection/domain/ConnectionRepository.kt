package com.tangai.memento.feature.connection.domain

import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    suspend fun redeemDirectInvite(code: String): Result<String>
    suspend fun getCurrentUserConnections(): Result<List<Connection>>
    suspend fun loadConnections(): Result<List<User>>
    fun observeConnections(): Flow<Result<List<User>>>
    suspend fun searchUsers(query: String): Result<List<User>>
}
