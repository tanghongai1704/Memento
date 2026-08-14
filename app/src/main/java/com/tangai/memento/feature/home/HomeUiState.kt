package com.tangai.memento.feature.home

import com.tangai.memento.data.model.FeedFilter
import com.tangai.memento.data.model.Post
import com.tangai.memento.data.model.User

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val connections: List<User> = emptyList(),
    val selectedFilter: FeedFilter = FeedFilter.All,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
