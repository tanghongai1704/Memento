package com.tangai.memento.feature.home.presentation.viewmodel

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.feature.home.domain.FeedFilter

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val connections: List<Connection> = emptyList(),
    val connectionLabels: Map<String, String> = emptyMap(),
    val authorLabels: Map<String, String> = emptyMap(),
    val selectedFilter: FeedFilter = FeedFilter.All,
    val mediaCacheRevision: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    fun labelFor(connection: Connection): String = connectionLabels[connection.id]
        ?: connection.name?.takeIf { it.isNotBlank() }
        ?: "Direct connection"
}
