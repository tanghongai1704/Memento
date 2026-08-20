package com.tangai.memento.feature.home.data

import com.tangai.memento.domain.model.AudienceType
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.domain.HomeRepository
import javax.inject.Inject

class FakeHomeRepository @Inject constructor() : HomeRepository {
    private val mockPosts = mutableListOf<Post>()
    private val currentUserId = "user_current"

    init {
        mockPosts.addAll(
            listOf(
                Post(
                    id = "post_1",
                    authorId = currentUserId,
                    audienceType = AudienceType.USER,
                    audienceId = "user_alice",
                    createdAt = System.currentTimeMillis() - 600000,
                    mediaType = MediaType.IMAGE
                ),
                Post(
                    id = "post_2",
                    authorId = "user_alice",
                    audienceType = AudienceType.USER,
                    audienceId = currentUserId,
                    createdAt = System.currentTimeMillis() - 300000,
                    mediaType = MediaType.VIDEO
                ),
                Post(
                    id = "post_3",
                    authorId = currentUserId,
                    audienceType = AudienceType.USER,
                    audienceId = "user_bob",
                    createdAt = System.currentTimeMillis() - 120000,
                    mediaType = MediaType.IMAGE
                )
            )
        )
    }

    override suspend fun loadPosts(): Result<List<Post>> {
        return Result.success(mockPosts)
    }

    override suspend fun loadConnections(): Result<List<User>> {
        return Result.success(
            listOf(
                User("user_alice", "Alice"),
                User("user_bob", "Bob"),
                User("user_charlie", "Charlie")
            )
        )
    }

    override fun getFilteredPosts(posts: List<Post>, filter: FeedFilter): List<Post> {
        return when (filter) {
            is FeedFilter.All -> {
                posts.filter { post ->
                    post.authorId == currentUserId || post.audienceId == currentUserId
                }.sortedByDescending { it.createdAt }
            }
            is FeedFilter.User -> {
                val userId = filter.userId
                posts.filter { post ->
                    (post.authorId == currentUserId && post.audienceId == userId) ||
                            (post.authorId == userId && post.audienceId == currentUserId)
                }.sortedByDescending { it.createdAt }
            }
        }
    }

    override fun addPost(post: Post): Result<Unit> {
        mockPosts.add(0, post)
        return Result.success(Unit)
    }
}
