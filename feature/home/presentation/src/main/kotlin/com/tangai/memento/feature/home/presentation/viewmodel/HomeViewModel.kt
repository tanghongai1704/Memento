package com.tangai.memento.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.domain.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val cachedHomeDataLoaded = CompletableDeferred<Unit>()

    init {
        viewModelScope.launch {
            try {
                loadCachedHomeData()
            } finally {
                cachedHomeDataLoaded.complete(Unit)
            }
        }
        viewModelScope.launch {
            cachedHomeDataLoaded.await()
            homeRepository.observeConnectedUsers().collect { result ->
                result.onSuccess { users ->
                    val connections = homeRepository.loadCachedConnections()
                        .getOrDefault(_uiState.value.connections)
                    // A remote disconnect can invalidate the Firestore post listeners before they
                    // can emit their final snapshot. Reloading from Room after connection
                    // reconciliation removes posts whose membership has just been revoked.
                    val posts = homeRepository.loadPosts()
                        .getOrDefault(_uiState.value.posts)
                    val usersById = users.associateBy(User::id)
                    val current = _uiState.value
                    val activeConnectionIds = connections.mapTo(mutableSetOf()) { it.id }
                    val selectedFilter = (current.selectedFilter as? FeedFilter.Connection)
                        ?.takeIf { it.connectionId in activeConnectionIds }
                        ?: FeedFilter.All
                    _uiState.value = current.copy(
                        posts = posts,
                        connections = connections,
                        connectionLabels = connectionLabels(connections, usersById),
                        connectionUsers = connectionUsers(connections, usersById),
                        authorProfiles = loadCachedAuthorProfiles(posts),
                        selectedFilter = selectedFilter,
                        connectionIdsWithMore = current.connectionIdsWithMore
                            .intersect(activeConnectionIds),
                        mediaCacheRevision = current.mediaCacheRevision + 1,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.message)
                }
            }
        }
        viewModelScope.launch {
            cachedHomeDataLoaded.await()
            homeRepository.observePosts().collect { result ->
                result.onSuccess { page ->
                    val authorProfiles = loadAuthorProfiles(page.posts)
                    _uiState.value = _uiState.value.copy(
                        posts = page.posts,
                        authorProfiles = authorProfiles,
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

    private suspend fun loadCachedHomeData() {
        val postsResult = homeRepository.loadPosts()
        val connectionsResult = homeRepository.loadCachedConnections()
        val connectedUsersResult = homeRepository.loadCachedConnectedUsers()
        val connections = connectionsResult.getOrElse { emptyList() }
        val posts = postsResult.getOrElse { emptyList() }
        val connectedUsersById = connectedUsersResult.getOrElse { emptyList() }.associateBy { it.id }

        _uiState.value = _uiState.value.copy(
            posts = posts,
            connections = connections,
            connectionLabels = connectionLabels(connections, connectedUsersById),
            connectionUsers = connectionUsers(connections, connectedUsersById),
            authorProfiles = loadCachedAuthorProfiles(posts),
            isLoading = false,
            errorMessage = postsResult.exceptionOrNull()?.message
                ?: connectionsResult.exceptionOrNull()?.message
                ?: connectedUsersResult.exceptionOrNull()?.message
        )
    }

    fun refresh() {
        viewModelScope.launch {
            val hasCachedContent = _uiState.value.posts.isNotEmpty() ||
                _uiState.value.connections.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasCachedContent,
                errorMessage = null
            )
            homeRepository.loadConnectedUsers()
                .onSuccess { loadCachedHomeData() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not sync your moments."
                    )
                }
        }
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
                        authorProfiles = loadAuthorProfiles(page.posts),
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

    fun canDelete(post: Post): Boolean = post.authorId == auth.currentUser?.uid

    fun deletePost(post: Post) {
        if (!canDelete(post)) return
        val postKey = post.key()
        if (postKey in _uiState.value.deletingPostKeys) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deletingPostKeys = _uiState.value.deletingPostKeys + postKey,
                errorMessage = null
            )
            homeRepository.deletePost(post)
                .onSuccess {
                    val posts = homeRepository.loadPosts().getOrDefault(
                        _uiState.value.posts.filterNot { it.key() == postKey }
                    )
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        authorProfiles = loadAuthorProfiles(posts),
                        deletingPostKeys = _uiState.value.deletingPostKeys - postKey,
                        mediaCacheRevision = _uiState.value.mediaCacheRevision + 1,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        deletingPostKeys = _uiState.value.deletingPostKeys - postKey,
                        errorMessage = error.message ?: "Could not delete this post."
                    )
                }
        }
    }

    fun getFilteredPosts(): List<Post> {
        return homeRepository.getFilteredPosts(_uiState.value.posts, _uiState.value.selectedFilter)
    }

    fun getPostAuthor(post: Post): User? = _uiState.value.authorProfiles[post.authorId]

    private suspend fun loadAuthorProfiles(posts: List<Post>): Map<String, User> =
        homeRepository.loadUsers(posts.mapTo(mutableSetOf(), Post::authorId))
            .getOrDefault(emptyList())
            .associateBy(User::id)

    private suspend fun loadCachedAuthorProfiles(posts: List<Post>): Map<String, User> =
        homeRepository.loadCachedUsers(posts.mapTo(mutableSetOf(), Post::authorId))
            .getOrDefault(emptyList())
            .associateBy(User::id)

    private fun connectionLabels(
        connections: List<com.tangai.memento.domain.model.Connection>,
        usersById: Map<String, User>
    ): Map<String, String> = connections.associate { connection ->
        val connectedUser = connection.members.asSequence()
            .mapNotNull { usersById[it.userId] }
            .firstOrNull()
        connection.id to (
            connection.name?.takeIf(String::isNotBlank)
                ?: connectedUser?.displayName
                ?: "Direct connection"
            )
    }

    private fun connectionUsers(
        connections: List<com.tangai.memento.domain.model.Connection>,
        usersById: Map<String, User>
    ): Map<String, User> = connections.mapNotNull { connection ->
        connection.members.asSequence()
            .mapNotNull { usersById[it.userId] }
            .firstOrNull()
            ?.let { connection.id to it }
    }.toMap()

    private fun Post.key(): String = "$connectionId:$id"
}
