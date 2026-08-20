package com.tangai.memento.feature.connection.domain

import com.tangai.memento.domain.model.User

interface ConnectionRepository {
    suspend fun loadConnections(): Result<List<User>>
}
