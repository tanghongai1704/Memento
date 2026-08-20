package com.tangai.memento.feature.home.presentation.viewmodel

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.home.domain.FeedFilter

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val connections: List<User> = emptyList(),
    val selectedFilter: FeedFilter = FeedFilter.All,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
