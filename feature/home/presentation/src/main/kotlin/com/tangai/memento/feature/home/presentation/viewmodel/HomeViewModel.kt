package com.tangai.memento.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.displayLabel
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

            val connectionsResult = homeRepository.loadConnections()
            val postsResult = homeRepository.loadPosts()
            val connectedUsersResult = homeRepository.loadConnectedUsers()
            val connections = connectionsResult.getOrElse { emptyList() }
            val connectedUsersById = connectedUsersResult.getOrElse { emptyList() }.associateBy { it.id }

            _uiState.value = _uiState.value.copy(
                posts = postsResult.getOrElse { emptyList() },
                connections = connections,
                connectionLabels = connections.associate { it.id to it.displayLabel(connectedUsersById) },
                isLoading = false,
                errorMessage = postsResult.exceptionOrNull()?.message
                    ?: connectionsResult.exceptionOrNull()?.message
                    ?: connectedUsersResult.exceptionOrNull()?.message
            )
        }
    }

    fun onFilterSelected(filter: FeedFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredPosts(): List<Post> {
        return homeRepository.getFilteredPosts(_uiState.value.posts, _uiState.value.selectedFilter)
    }

    fun getPostLabel(post: Post): String =
        _uiState.value.connections.find { it.id == post.connectionId }
            ?.let(_uiState.value::labelFor) ?: "Direct connection"
}
