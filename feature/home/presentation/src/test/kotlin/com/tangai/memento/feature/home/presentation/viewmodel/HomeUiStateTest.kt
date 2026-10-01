package com.tangai.memento.feature.home.presentation.viewmodel

import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionStatus
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.PostStatus
import com.tangai.memento.domain.model.PostType
import com.tangai.memento.feature.home.domain.FeedFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun `suggestions prioritize shared activity before new connections`() {
        val state = HomeUiState(
            connections = listOf(
                connection(id = "old-post", createdAt = 100, lastPostAt = 200),
                connection(id = "new-connection", createdAt = 300),
                connection(id = "new-post", createdAt = 50, lastPostAt = 400)
            )
        )

        assertEquals(
            listOf("new-post", "old-post", "new-connection"),
            state.suggestedConnections.map(Connection::id)
        )
        assertFalse(state.hasMoreConnections)
    }

    @Test
    fun `home shows at most five recent connections and exposes more`() {
        val state = HomeUiState(
            connections = (1L..6L).map { index ->
                connection(id = "connection-$index", createdAt = index)
            }
        )

        assertEquals(5, state.suggestedConnections.size)
        assertEquals("connection-6", state.suggestedConnections.first().id)
        assertTrue(state.hasMoreConnections)
    }

    @Test
    fun `selected connection outside recent five is appended without hiding suggestions`() {
        val state = HomeUiState(
            connections = (1L..6L).map { index ->
                connection(id = "connection-$index", createdAt = index)
            },
            selectedFilter = FeedFilter.Connection("connection-1")
        )

        assertEquals(
            listOf(
                "connection-6",
                "connection-5",
                "connection-4",
                "connection-3",
                "connection-2",
                "connection-1"
            ),
            state.suggestedConnections.map(Connection::id)
        )
    }

    @Test
    fun `own direct post identifies the recipient`() {
        val connection = connection(id = "direct", createdAt = 1)
        val state = HomeUiState(
            currentUserId = "me",
            connections = listOf(connection),
            connectionLabels = mapOf("direct" to "Lan")
        )
        val post = post(authorId = "me", connectionId = "direct")

        assertEquals("You", state.authorLabelFor(post))
        assertEquals("Shared with Lan", state.sharingLabelFor(post))
    }

    @Test
    fun `received direct post identifies the current user as recipient`() {
        val state = HomeUiState(
            currentUserId = "me",
            connections = listOf(connection(id = "direct", createdAt = 1))
        )

        assertEquals(
            "Shared with you",
            state.sharingLabelFor(post(authorId = "friend", connectionId = "direct"))
        )
    }

    private fun connection(
        id: String,
        createdAt: Long,
        lastPostAt: Long? = null
    ) = Connection(
        id = id,
        type = ConnectionType.DIRECT,
        members = emptyList(),
        status = ConnectionStatus.ACTIVE,
        lastPostAt = lastPostAt,
        createdBy = "owner",
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private fun post(authorId: String, connectionId: String) = Post(
        id = "post",
        connectionId = connectionId,
        authorId = authorId,
        postType = PostType.PHOTO,
        layoutType = LayoutType.SINGLE,
        clientCreatedAt = 1,
        status = PostStatus.ACTIVE
    )
}
