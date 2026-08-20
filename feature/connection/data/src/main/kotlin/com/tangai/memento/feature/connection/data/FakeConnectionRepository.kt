package com.tangai.memento.feature.connection.data

import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import javax.inject.Inject

class FakeConnectionRepository @Inject constructor() : ConnectionRepository {
    override suspend fun loadConnections(): Result<List<User>> {
        return Result.success(
            listOf(
                User("user_alice", "Alice"),
                User("user_bob", "Bob"),
                User("user_charlie", "Charlie")
            )
        )
    }
}
