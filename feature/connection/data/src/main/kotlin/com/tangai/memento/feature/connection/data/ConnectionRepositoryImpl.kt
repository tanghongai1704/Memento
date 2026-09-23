package com.tangai.memento.feature.connection.data

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.database.model.toEntity
import com.tangai.memento.domain.model.*
import com.tangai.memento.feature.connection.data.source.ConnectionFirestoreDataSource
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MementoDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val source: ConnectionFirestoreDataSource
) : ConnectionRepository {
    override suspend fun redeemDirectInvite(code: String): Result<String> = runCatching {
        val connectionId = source.redeemDirectInvite(code)
        // The backend transaction has already committed. A cache refresh failure must not
        // make the UI report that redeem failed and encourage reuse of a consumed code.
        getCurrentUserConnections()
        connectionId
    }

    override suspend fun disconnectDirect(connectionId: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("User is not signed in.")
        val updatedAt = source.disconnectDirect(connectionId)
        check(firebaseAuth.currentUser?.uid == uid) { "Account changed during disconnect." }
        database.withTransaction {
            database.connectionDao().markClosed(connectionId, updatedAt)
            database.connectionMemberDao().markMembersLeft(connectionId, updatedAt)
        }
        clearConnectionMediaCache(connectionId)
    }

    override suspend fun getCurrentUserConnections(): Result<List<Connection>> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("User is not signed in.")
        val connections = source.getConnectionsForCurrentUser()
        check(firebaseAuth.currentUser?.uid == uid) { "Account changed during sync." }
        val revokedConnectionIds = mutableListOf<String>()
        database.withTransaction {
            // Revoke cached access when a connection closes or membership disappears.
            database.connectionMemberDao().getActiveMembershipsForUser(uid).forEach { membership ->
                if (connections.none { it.id == membership.connectionId }) {
                    database.connectionMemberDao().upsertMember(membership.copy(status = MemberStatus.LEFT))
                    revokedConnectionIds += membership.connectionId
                }
            }
            connections.forEach { connection ->
                database.connectionDao().upsertConnection(connection.toEntity())
                connection.members.forEach { database.connectionMemberDao().upsertMember(it.toEntity(connection.id)) }
            }
        }
        revokedConnectionIds.forEach(::clearConnectionMediaCache)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeConnections(): Flow<Result<List<User>>> =
        source.observeCurrentUserConnectionChanges()
            .map {
                loadConnections().getOrThrow().map(User::id).distinct()
            }
            // Closing a connection revokes both members' read access immediately. Firestore can
            // therefore end the listener with PERMISSION_DENIED instead of delivering a REMOVED
            // change. Reconcile with a fresh query before restarting the listener so the member
            // who did not initiate the disconnect also loses the cached connection right away.
            .retryWhen { _, _ ->
                emit(loadConnections().getOrThrow().map(User::id).distinct())
                delay(CONNECTION_LISTENER_RETRY_DELAY_MS)
                true
            }
            .flatMapLatest { userIds ->
                if (userIds.isEmpty()) {
                    flowOf(Result.success(emptyList()))
                } else {
                    combine(userIds.map(source::observeUser)) { profiles ->
                        val users = profiles.filterNotNull()
                        users.forEach { database.userDao().upsertUser(it.toEntity()) }
                        Result.success(users)
                    }
                }
            }
            .catch { emit(Result.failure(it)) }

    override suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("User is not signed in.")
        source.searchUsers(query).filterNot { it.id == uid }.also { users ->
            check(firebaseAuth.currentUser?.uid == uid) { "Account changed during search." }
            users.forEach { database.userDao().upsertUser(it.toEntity()) }
        }
    }

    private fun clearConnectionMediaCache(connectionId: String) {
        if (!connectionId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) return
        File(context.filesDir, "pending_media/$connectionId").deleteRecursively()
    }

    private companion object {
        const val CONNECTION_LISTENER_RETRY_DELAY_MS = 1_000L
    }
}
