package com.tangai.memento.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.Post
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.domain.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val postsResult = homeRepository.loadPosts()
            val connectionsResult = homeRepository.loadConnections()

            _uiState.value = _uiState.value.copy(
                posts = postsResult.getOrElse { emptyList() },
                connections = connectionsResult.getOrElse { emptyList() },
                isLoading = false
            )
        }
    }

    fun onFilterSelected(filter: FeedFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredPosts(): List<Post> {
        return homeRepository.getFilteredPosts(_uiState.value.posts, _uiState.value.selectedFilter)
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
        val result = homeRepository.addPost(post)
        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                posts = listOf(post) + _uiState.value.posts
            )
        }
    }
}
