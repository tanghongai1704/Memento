package com.tangai.memento.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.data.model.AudienceType
import com.tangai.memento.data.model.FeedFilter
import com.tangai.memento.data.model.MediaType
import com.tangai.memento.data.model.Post
import com.tangai.memento.data.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            delay(500)

            val currentUserId = "user_current"
            val connections = listOf(
                User("user_alice", "Alice"),
                User("user_bob", "Bob"),
                User("user_charlie", "Charlie")
            )

            val fakePosts = listOf(
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

            _uiState.value = _uiState.value.copy(
                posts = fakePosts,
                connections = connections,
                isLoading = false
            )
        }
    }

    fun onFilterSelected(filter: FeedFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredPosts(): List<Post> {
        val currentUserId = "user_current"
        val posts = _uiState.value.posts
        val filter = _uiState.value.selectedFilter

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

    fun getPostLabel(post: Post, currentUserId: String = "user_current"): String {
        val sender = if (post.authorId == currentUserId) "You" else getUsername(post.authorId)
        val recipient = getUsername(post.audienceId)
        return "$sender → $recipient"
    }

    private fun getUsername(userId: String): String {
        return _uiState.value.connections.find { it.id == userId }?.username ?: "Unknown"
    }

    fun addPost(post: Post) {
        _uiState.value = _uiState.value.copy(
            posts = listOf(post) + _uiState.value.posts
        )
    }
}

