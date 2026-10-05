package com.tangai.memento.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionLabelTest {
    private val member = ConnectionMember(
        userId = "friend",
        role = MemberRole.MEMBER,
        status = MemberStatus.ACTIVE,
        joinedAt = 1L,
        leftAt = null
    )

    @Test
    fun explicitConnectionNameHasPriority() {
        val connection = connection(name = "Family")

        assertEquals("Family", connection.displayLabel(mapOf("friend" to friend())))
    }

    @Test
    fun directConnectionUsesActiveMemberProfile() {
        val connection = connection(name = null)

        assertEquals("Alice (@alice)", connection.displayLabel(mapOf("friend" to friend())))
    }

    @Test
    fun missingProfileFallsBackToConnectionType() {
        assertEquals("Direct connection", connection(name = null).displayLabel(emptyMap()))
        assertEquals(
            "Group",
            connection(name = null, type = ConnectionType.GROUP).displayLabel(emptyMap())
        )
    }

    private fun connection(name: String?, type: ConnectionType = ConnectionType.DIRECT) = Connection(
        id = "connection",
        type = type,
        name = name,
        status = ConnectionStatus.ACTIVE,
        createdBy = "me",
        createdAt = 1L,
        updatedAt = 1L,
        members = listOf(member)
    )

    private fun friend() = User(
        id = "friend",
        username = "alice",
        displayName = "Alice"
    )
}
