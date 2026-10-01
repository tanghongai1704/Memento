package com.tangai.memento.feature.connection.domain

import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.User
import kotlinx.coroutines.flow.Flow

data class CachedConnectionUsers(
    val users: List<User>,
    val connectionIdsByUserId: Map<String, String>
)

interface ConnectionRepository {
    suspend fun redeemDirectInvite(code: String): Result<String>
    suspend fun disconnectDirect(connectionId: String): Result<Unit>
    suspend fun getCurrentUserConnections(): Result<List<Connection>>
    suspend fun loadCachedConnections(): Result<List<Connection>>
    suspend fun loadConnections(): Result<List<User>>
    suspend fun loadCachedConnectionUsers(): Result<CachedConnectionUsers>
    fun observeConnections(): Flow<Result<List<User>>>
    suspend fun searchUsers(query: String): Result<List<User>>
}
