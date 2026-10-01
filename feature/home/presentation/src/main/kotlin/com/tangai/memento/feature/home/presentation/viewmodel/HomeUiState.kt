package com.tangai.memento.feature.home.presentation.viewmodel

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionType
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
    val currentUserId: String? = null,
    val loadingMediaPostKeys: Set<String> = emptySet(),
    val failedMediaPostKeys: Set<String> = emptySet(),
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

    fun authorLabelFor(post: Post): String = if (post.authorId == currentUserId) {
        "You"
    } else {
        authorProfiles[post.authorId]?.displayName?.takeIf(String::isNotBlank)
            ?: "Unknown author"
    }

    fun sharingLabelFor(post: Post): String {
        val connection = connections.firstOrNull { it.id == post.connectionId }
        return when {
            connection?.type == ConnectionType.GROUP -> "Shared in ${labelFor(connection)}"
            post.authorId == currentUserId && connection != null -> "Shared with ${labelFor(connection)}"
            post.authorId == currentUserId -> "Shared privately"
            else -> "Shared with you"
        }
    }

    fun postKey(post: Post): String = "${post.connectionId}:${post.id}"
}

private const val HOME_SUGGESTION_LIMIT = 5
