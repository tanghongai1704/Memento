package com.tangai.memento.feature.connection.domain

import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionMember
import com.tangai.memento.domain.model.ConnectionRequest
import com.tangai.memento.domain.model.User

interface ConnectionRepository {
    suspend fun createDirectConnection(userId: String): Result<Unit>
    suspend fun getCurrentUserConnections(): Result<List<Connection>>
    suspend fun getConnectionBetweenUsers(firstUserId: String, secondUserId: String): Result<Connection?>
    suspend fun getConnectionMembers(connectionId: String): Result<List<ConnectionMember>>

    suspend fun sendConnectionRequest(
        receiverId: String,
        message: String? = null
    ): Result<Unit>

    suspend fun getPendingRequests(): Result<List<ConnectionRequest>>
    suspend fun acceptConnectionRequest(requestId: String): Result<Unit>
    suspend fun rejectConnectionRequest(requestId: String): Result<Unit>
    suspend fun hasDuplicateConnection(firstUserId: String, secondUserId: String): Result<Boolean>
    suspend fun hasDuplicateRequest(firstUserId: String, secondUserId: String): Result<Boolean>

    suspend fun loadConnections(): Result<List<User>>
    suspend fun searchUsers(query: String): Result<List<User>>
}
