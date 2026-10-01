package com.tangai.memento.feature.home.presentation.viewmodel

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.home.domain.FeedFilter

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val connections: List<Connection> = emptyList(),
    val connectionLabels: Map<String, String> = emptyMap(),
    val connectionUsers: Map<String, User> = emptyMap(),
    val authorProfiles: Map<String, User> = emptyMap(),
    val selectedFilter: FeedFilter = FeedFilter.All,
    val mediaCacheRevision: Long = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val connectionIdsWithMore: Set<String> = emptySet(),
    val deletingPostKeys: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    val orderedConnections: List<Connection>
        get() = connections.sortedWith(
            compareByDescending<Connection> { it.lastPostAt != null }
                .thenByDescending { it.lastPostAt ?: Long.MIN_VALUE }
                .thenByDescending(Connection::createdAt)
                .thenBy { labelFor(it).lowercase() }
                .thenBy(Connection::id)
        )

    val suggestedConnections: List<Connection>
        get() {
            val recent = orderedConnections.take(HOME_SUGGESTION_LIMIT)
            val selectedId = (selectedFilter as? FeedFilter.Connection)?.connectionId
                ?: return recent
            if (recent.any { it.id == selectedId }) return recent
            val selected = orderedConnections.firstOrNull { it.id == selectedId }
                ?: return recent
            return recent + selected
        }

    val hasMoreConnections: Boolean
        get() = connections.size > HOME_SUGGESTION_LIMIT

    val hasMorePosts: Boolean
        get() = when (val filter = selectedFilter) {
            FeedFilter.All -> connectionIdsWithMore.isNotEmpty()
            is FeedFilter.Connection -> filter.connectionId in connectionIdsWithMore
        }

    fun labelFor(connection: Connection): String = connectionLabels[connection.id]
        ?: connection.name?.takeIf { it.isNotBlank() }
        ?: "Direct connection"
}

private const val HOME_SUGGESTION_LIMIT = 5
