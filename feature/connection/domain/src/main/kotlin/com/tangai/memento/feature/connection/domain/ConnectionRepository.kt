package com.tangai.memento.feature.connection.domain

import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.User

interface ConnectionRepository {
    suspend fun getCurrentUserConnections(): Result<List<Connection>>
    suspend fun loadConnections(): Result<List<User>>
    suspend fun searchUsers(query: String): Result<List<User>>
}
