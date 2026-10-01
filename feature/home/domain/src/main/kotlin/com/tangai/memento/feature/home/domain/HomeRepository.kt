package com.tangai.memento.feature.home.domain

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.User
import kotlinx.coroutines.flow.Flow

data class PostFeedPage(
    val posts: List<Post>,
    val connectionIdsWithMore: Set<String>
)

interface HomeRepository {
    suspend fun loadPosts(): Result<List<Post>>
    fun observePosts(): Flow<Result<PostFeedPage>>
    suspend fun loadOlderPosts(connectionId: String? = null): Result<PostFeedPage>
    suspend fun deletePost(post: Post): Result<Unit>
    suspend fun loadUsers(userIds: Set<String>): Result<List<User>>
    suspend fun loadCachedUsers(userIds: Set<String>): Result<List<User>>
    suspend fun loadConnections(): Result<List<Connection>>
    suspend fun loadCachedConnections(): Result<List<Connection>>
    suspend fun loadConnectedUsers(): Result<List<User>>
    suspend fun loadCachedConnectedUsers(): Result<List<User>>
    fun observeConnectedUsers(): Flow<Result<List<User>>>
    fun getFilteredPosts(posts: List<Post>, filter: FeedFilter): List<Post>
}
