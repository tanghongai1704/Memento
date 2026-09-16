package com.tangai.memento.feature.home.domain

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.User
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    suspend fun loadPosts(): Result<List<Post>>
    fun observePosts(): Flow<Result<List<Post>>>
    suspend fun loadUsers(userIds: Set<String>): Result<List<User>>
    suspend fun loadConnections(): Result<List<Connection>>
    suspend fun loadConnectedUsers(): Result<List<User>>
    fun observeConnectedUsers(): Flow<Result<List<User>>>
    fun getFilteredPosts(posts: List<Post>, filter: FeedFilter): List<Post>
}
