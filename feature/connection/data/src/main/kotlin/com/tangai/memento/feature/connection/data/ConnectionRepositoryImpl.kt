package com.tangai.memento.feature.connection.data

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.database.model.toEntity
import com.tangai.memento.domain.model.*
import com.tangai.memento.feature.connection.data.source.ConnectionFirestoreDataSource
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    private val database: MementoDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val source: ConnectionFirestoreDataSource
) : ConnectionRepository {
    override suspend fun getCurrentUserConnections(): Result<List<Connection>> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("User is not signed in.")
        val connections = source.getConnectionsForCurrentUser()
        check(firebaseAuth.currentUser?.uid == uid) { "Account changed during sync." }
        database.withTransaction {
            // Revoke cached access when a connection closes or membership disappears.
            database.connectionMemberDao().getActiveMembershipsForUser(uid).forEach { membership ->
                if (connections.none { it.id == membership.connectionId }) {
                    database.connectionMemberDao().upsertMember(membership.copy(status = "LEFT"))
                }
            }
            connections.forEach { connection ->
                database.connectionDao().upsertConnection(connection.toEntity())
                connection.members.forEach { database.connectionMemberDao().upsertMember(it.toEntity(connection.id)) }
            }
        }
        connections
    }

    override suspend fun loadConnections(): Result<List<User>> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("User is not signed in.")
        getCurrentUserConnections().getOrThrow().flatMap { it.members }
            .filter { it.status == MemberStatus.ACTIVE && it.userId != uid }
            .map { it.userId }.distinct().mapNotNull { source.getUser(it) }
            .also { users ->
                check(firebaseAuth.currentUser?.uid == uid) { "Account changed during sync." }
                users.forEach { database.userDao().upsertUser(it.toEntity()) }
            }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("User is not signed in.")
        source.searchUsers(query).filterNot { it.id == uid }.also { users ->
            check(firebaseAuth.currentUser?.uid == uid) { "Account changed during search." }
            users.forEach { database.userDao().upsertUser(it.toEntity()) }
        }
    }
}
