package com.tangai.memento.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.domain.model.displayLabel
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.domain.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadHomeData()
        }
        viewModelScope.launch {
            homeRepository.observeConnectedUsers().collect { result ->
                result.onSuccess { users ->
                    val connections = homeRepository.loadConnections()
                        .getOrDefault(_uiState.value.connections)
                    val usersById = users.associateBy(User::id)
                    val current = _uiState.value
                    _uiState.value = current.copy(
                        connections = connections,
                        connectionLabels = connections.associate {
                            connection -> connection.id to connection.displayLabel(usersById)
                        }
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.message)
                }
            }
        }
        viewModelScope.launch {
            homeRepository.observePosts().collect { result ->
                result.onSuccess { page ->
                    val authorLabels = loadAuthorLabels(page.posts)
                    _uiState.value = _uiState.value.copy(
                        posts = page.posts,
                        authorLabels = authorLabels,
                        mediaCacheRevision = _uiState.value.mediaCacheRevision + 1,
                        isLoading = false,
                        connectionIdsWithMore = page.connectionIdsWithMore,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not sync shared photos."
                    )
                }
            }
        }
    }

    private suspend fun loadHomeData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val connectionsResult = homeRepository.loadConnections()
        val postsResult = homeRepository.loadPosts()
        val connectedUsersResult = homeRepository.loadConnectedUsers()
        val connections = connectionsResult.getOrElse { emptyList() }
        val posts = postsResult.getOrElse { emptyList() }
        val connectedUsersById = connectedUsersResult.getOrElse { emptyList() }.associateBy { it.id }

        _uiState.value = _uiState.value.copy(
            posts = posts,
            connections = connections,
            connectionLabels = connections.associate { it.id to it.displayLabel(connectedUsersById) },
            authorLabels = loadAuthorLabels(posts),
            isLoading = false,
            errorMessage = postsResult.exceptionOrNull()?.message
                ?: connectionsResult.exceptionOrNull()?.message
                ?: connectedUsersResult.exceptionOrNull()?.message
        )
    }

    fun onFilterSelected(filter: FeedFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun loadOlderPosts() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePosts) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            val connectionId = (_uiState.value.selectedFilter as? FeedFilter.Connection)?.connectionId
            homeRepository.loadOlderPosts(connectionId)
                .onSuccess { page ->
                    _uiState.value = _uiState.value.copy(
                        posts = page.posts,
                        authorLabels = loadAuthorLabels(page.posts),
                        mediaCacheRevision = _uiState.value.mediaCacheRevision + 1,
                        isLoadingMore = false,
                        connectionIdsWithMore = page.connectionIdsWithMore,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Could not load older photos."
                    )
                }
        }
    }

    fun getFilteredPosts(): List<Post> {
        return homeRepository.getFilteredPosts(_uiState.value.posts, _uiState.value.selectedFilter)
    }

    fun getPostLabel(post: Post): String =
        _uiState.value.authorLabels[post.authorId] ?: "Unknown author"

    private suspend fun loadAuthorLabels(posts: List<Post>): Map<String, String> =
        homeRepository.loadUsers(posts.mapTo(mutableSetOf(), Post::authorId))
            .getOrDefault(emptyList())
            .associate { user -> user.id to "${user.displayName} (@${user.username})" }
}
