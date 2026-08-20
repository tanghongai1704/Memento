package com.tangai.memento.feature.home.domain

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User

interface HomeRepository {
    suspend fun loadPosts(): Result<List<Post>>
    suspend fun loadConnections(): Result<List<User>>
    fun getFilteredPosts(posts: List<Post>, filter: FeedFilter): List<Post>
    fun addPost(post: Post): Result<Unit>
}
