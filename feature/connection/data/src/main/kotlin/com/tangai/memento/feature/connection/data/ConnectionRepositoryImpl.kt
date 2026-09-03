package com.tangai.memento.feature.connection.data

import com.tangai.memento.database.dao.ConnectionDao
import com.tangai.memento.database.dao.ConnectionMemberDao
import com.tangai.memento.database.dao.ConnectionRequestDao
import com.tangai.memento.database.dao.UserDao
import com.tangai.memento.database.model.ConnectionEntity
import com.tangai.memento.database.model.ConnectionMemberEntity
import com.tangai.memento.database.model.ConnectionRequestEntity
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.database.model.toEntity
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionMember
import com.tangai.memento.domain.model.ConnectionRequest
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.MemberStatus
import com.tangai.memento.domain.model.RequestStatus
import com.tangai.memento.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.feature.connection.data.mapper.toConnectionException
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val connectionMemberDao: ConnectionMemberDao,
    private val connectionRequestDao: ConnectionRequestDao,
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth
) : ConnectionRepository {
    override suspend fun createDirectConnection(userId: String): Result<Unit> = runCatching {
        val currentUserId = requireCurrentUserId()
        require(currentUserId != userId)
        require(!hasDuplicateConnection(currentUserId, userId).getOrThrow())

        val now = System.currentTimeMillis()
        val connectionId = buildDirectConnectionId(currentUserId, userId)
        connectionDao.upsertConnection(
            ConnectionEntity(
                id = connectionId,
                type = ConnectionType.DIRECT.name,
                name = null,
                description = null,
                createdBy = currentUserId,
                createdAt = now,
                updatedAt = now
            )
        )
        connectionMemberDao.upsertMembers(
            listOf(
                ConnectionMemberEntity(connectionId, currentUserId, "member", now, MemberStatus.ACTIVE.name),
                ConnectionMemberEntity(connectionId, userId, "member", now, MemberStatus.ACTIVE.name)
            )
        )
    }

    override suspend fun getCurrentUserConnections(): Result<List<Connection>> = runCatching {
        val currentUserId = requireCurrentUserId()
        val memberships = connectionMemberDao.getActiveMembershipsForUser(currentUserId)
        memberships.mapNotNull { membership ->
            connectionDao.getConnectionById(membership.connectionId)?.let { connection ->
                val members = connectionMemberDao.getMembersByConnectionId(connection.id).map { it.toDomain() }
                connection.toDomain(members)
            }
        }
    }

    override suspend fun getConnectionBetweenUsers(firstUserId: String, secondUserId: String): Result<Connection?> = runCatching {
        val firstMemberships = connectionMemberDao.getActiveMembershipsForUser(firstUserId)
        val connectionId = firstMemberships
            .map { it.connectionId }
            .firstOrNull { candidateId ->
                connectionMemberDao.getMembersByConnectionId(candidateId)
                    .map { it.userId }
                    .containsAll(listOf(firstUserId, secondUserId))
            }
        connectionId?.let { id ->
            val connection = connectionDao.getConnectionById(id) ?: return@runCatching null
            val members = connectionMemberDao.getMembersByConnectionId(id).map { it.toDomain() }
            connection.toDomain(members)
        }
    }

    override suspend fun getConnectionMembers(connectionId: String): Result<List<ConnectionMember>> = runCatching {
        connectionMemberDao.getMembersByConnectionId(connectionId).map { it.toDomain() }
    }

    override suspend fun sendConnectionRequest(receiverId: String, message: String?): Result<Unit> = runCatching {
        val senderId = requireCurrentUserId()
        require(senderId != receiverId)
        require(!hasDuplicateConnection(senderId, receiverId).getOrThrow())
        require(!hasDuplicateRequest(senderId, receiverId).getOrThrow())

        val now = System.currentTimeMillis()
        connectionRequestDao.upsertRequest(
            ConnectionRequestEntity(
                id = "${senderId}_${receiverId}_$now",
                connectionId = null,
                senderId = senderId,
                receiverId = receiverId,
                connectionType = ConnectionType.DIRECT.name,
                message = message,
                status = RequestStatus.PENDING.name,
                createdAt = now,
                respondedAt = null
            )
        )
    }

    override suspend fun getPendingRequests(): Result<List<ConnectionRequest>> = runCatching {
        val currentUserId = requireCurrentUserId()
        connectionRequestDao.getPendingRequestsForUser(currentUserId).map { it.toDomain() }
    }

    override suspend fun acceptConnectionRequest(requestId: String): Result<Unit> = runCatching {
        val request = connectionRequestDao.getRequestById(requestId)
            ?: throw IllegalStateException("Request not found.")
        require(request.status == RequestStatus.PENDING.name)

        createDirectConnection(request.senderId).getOrThrow()
        connectionRequestDao.updateRequest(
            request.copy(
                status = RequestStatus.ACCEPTED.name,
                respondedAt = System.currentTimeMillis(),
                connectionId = buildDirectConnectionId(request.senderId, request.receiverId)
            )
        )
    }

    override suspend fun rejectConnectionRequest(requestId: String): Result<Unit> = runCatching {
        val request = connectionRequestDao.getRequestById(requestId)
            ?: throw IllegalStateException("Request not found.")
        require(request.status == RequestStatus.PENDING.name)

        connectionRequestDao.updateRequest(
            request.copy(
                status = RequestStatus.REJECTED.name,
                respondedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun hasDuplicateConnection(firstUserId: String, secondUserId: String): Result<Boolean> = runCatching {
        getConnectionBetweenUsers(firstUserId, secondUserId).getOrThrow() != null
    }

    override suspend fun hasDuplicateRequest(firstUserId: String, secondUserId: String): Result<Boolean> = runCatching {
        connectionRequestDao.checkPendingRequest(firstUserId, secondUserId) != null ||
            connectionRequestDao.checkPendingRequest(secondUserId, firstUserId) != null
    }

    override suspend fun loadConnections(): Result<List<User>> = runCatching {
        val currentUserId = requireCurrentUserId()
        connectionMemberDao.getActiveMembershipsForUser(currentUserId)
            .mapNotNull { membership ->
                if (membership.userId == currentUserId) null else userDao.getUserById(membership.userId)?.toDomain()
            }
            .distinctBy(User::id)
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val currentUserId = requireCurrentUserId()
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return@runCatching emptyList()

        userDao.getAllUsers()
            .map { it.toDomain() }
            .filterNot { it.id == currentUserId }
            .filter {
                it.username.lowercase().contains(normalizedQuery) ||
                    it.email.lowercase().contains(normalizedQuery) ||
                    it.id.lowercase().contains(normalizedQuery)
            }
            .sortedBy { it.username.lowercase() }
    }

    private fun requireCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("User is not signed in.")
    }

    private fun buildDirectConnectionId(firstUserId: String, secondUserId: String): String {
        return listOf(firstUserId, secondUserId).sorted().joinToString("_")
    }
}
