package com.tangai.memento.feature.connection.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.tangai.memento.feature.auth.data.source.util.awaitTask
import com.tangai.memento.feature.connection.data.model.ConnectionMemberRemote
import com.tangai.memento.feature.connection.data.model.ConnectionRemote
import com.tangai.memento.feature.connection.data.model.ConnectionRequestRemote
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.ConnectionStatus
import com.tangai.memento.domain.model.MemberStatus
import com.tangai.memento.domain.model.RequestStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class ConnectionFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private fun requireUserId(): String = firebaseAuth.currentUser?.uid
        ?: throw IllegalStateException("User is not signed in.")

    suspend fun upsertConnection(connection: ConnectionRemote): Result<Unit> = runCatching {
        firestore.collection("connections")
            .document(connection.id)
            .set(
                mapOf(
                    "connectionId" to connection.id,
                    "type" to connection.type.name,
                    "createdBy" to connection.createdBy,
                    "status" to connection.status.name,
                    "name" to connection.name,
                    "description" to connection.description,
                    "createdAt" to connection.createdAt,
                    "updatedAt" to connection.updatedAt
                )
            )
            .awaitTask()
    }

    suspend fun upsertMembers(members: List<ConnectionMemberRemote>): Result<Unit> = runCatching {
        members.forEach { member ->
            firestore.collection("connection_members")
                .document("${member.connectionId}_${member.userId}")
                .set(
                    mapOf(
                        "connectionId" to member.connectionId,
                        "userId" to member.userId,
                        "role" to member.role,
                        "joinedAt" to member.joinedAt,
                        "status" to member.status.name
                    )
                )
                .awaitTask()
        }
    }

    suspend fun upsertRequest(request: ConnectionRequestRemote): Result<Unit> = runCatching {
        firestore.collection("connection_requests")
            .document(request.id)
            .set(
                mapOf(
                    "requestId" to request.id,
                    "connectionId" to request.connectionId,
                    "senderId" to request.senderId,
                    "receiverId" to request.receiverId,
                    "connectionType" to request.connectionType.name,
                    "status" to request.status.name,
                    "message" to request.message,
                    "createdAt" to request.createdAt,
                    "updatedAt" to request.updatedAt
                )
            )
            .awaitTask()
    }

    suspend fun updateRequestStatus(requestId: String, status: String, connectionId: String? = null): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to System.currentTimeMillis()
        )
        if (connectionId != null) {
            updates["connectionId"] = connectionId
        }
        firestore.collection("connection_requests")
            .document(requestId)
            .update(updates)
            .awaitTask()
    }

    suspend fun getConnectionsForCurrentUser(): Result<List<ConnectionRemote>> = runCatching {
        val userId = requireUserId()
        firestore.collection("connections")
            .whereEqualTo("createdBy", userId)
            .get()
            .awaitTask()
            .documents
            .mapNotNull { it.toConnectionRemote() }
    }

    suspend fun getRequestsForCurrentUser(): Result<List<ConnectionRequestRemote>> = runCatching {
        val userId = requireUserId()
        firestore.collection("connection_requests")
            .whereEqualTo("receiverId", userId)
            .get()
            .awaitTask()
            .documents
            .mapNotNull { it.toRequestRemote() }
    }

    suspend fun getSentRequestsForCurrentUser(): Result<List<ConnectionRequestRemote>> = runCatching {
        val userId = requireUserId()
        firestore.collection("connection_requests")
            .whereEqualTo("senderId", userId)
            .get()
            .awaitTask()
            .documents
            .mapNotNull { it.toRequestRemote() }
    }

    suspend fun getMembersForConnection(connectionId: String): Result<List<ConnectionMemberRemote>> = runCatching {
        firestore.collection("connection_members")
            .whereEqualTo("connectionId", connectionId)
            .get()
            .awaitTask()
            .documents
            .mapNotNull { it.toMemberRemote(connectionId) }
    }

    fun observeConnectionsForCurrentUser(): Flow<List<ConnectionRemote>> = callbackFlow {
        val userId = requireUserId()
        val registration: ListenerRegistration = firestore.collection("connections")
            .whereEqualTo("createdBy", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toConnectionRemote() })
            }

        awaitClose { registration.remove() }
    }

    fun observeIncomingRequestsForCurrentUser(): Flow<List<ConnectionRequestRemote>> = callbackFlow {
        val userId = requireUserId()
        val registration = firestore.collection("connection_requests")
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toRequestRemote() })
            }

        awaitClose { registration.remove() }
    }

    fun observeSentRequestsForCurrentUser(): Flow<List<ConnectionRequestRemote>> = callbackFlow {
        val userId = requireUserId()
        val registration = firestore.collection("connection_requests")
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toRequestRemote() })
            }

        awaitClose { registration.remove() }
    }

    fun observeMembersForConnection(connectionId: String): Flow<List<ConnectionMemberRemote>> = callbackFlow {
        val registration = firestore.collection("connection_members")
            .whereEqualTo("connectionId", connectionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toMemberRemote(connectionId) })
            }

        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toConnectionRemote(): ConnectionRemote? {
        val id = getString("connectionId") ?: id
        val type = getString("type")?.let { ConnectionType.valueOf(it) } ?: ConnectionType.DIRECT
        val createdBy = getString("createdBy") ?: return null
        val status = getString("status")?.let { ConnectionStatus.valueOf(it) } ?: ConnectionStatus.ACTIVE
        val createdAt = getLong("createdAt") ?: 0L
        val updatedAt = getLong("updatedAt") ?: createdAt
        return ConnectionRemote(
            id = id,
            type = type,
            createdBy = createdBy,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            name = getString("name"),
            description = getString("description")
        )
    }

    private fun DocumentSnapshot.toRequestRemote(): ConnectionRequestRemote? {
        val id = getString("requestId") ?: id
        val senderId = getString("senderId") ?: return null
        val receiverId = getString("receiverId") ?: return null
        val connectionType = getString("connectionType")?.let { ConnectionType.valueOf(it) } ?: ConnectionType.DIRECT
        val status = getString("status")?.let { RequestStatus.valueOf(it) } ?: RequestStatus.PENDING
        val createdAt = getLong("createdAt") ?: 0L
        val updatedAt = getLong("updatedAt") ?: createdAt
        return ConnectionRequestRemote(
            id = id,
            connectionId = getString("connectionId"),
            senderId = senderId,
            receiverId = receiverId,
            connectionType = connectionType,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            message = getString("message")
        )
    }

    private fun DocumentSnapshot.toMemberRemote(fallbackConnectionId: String): ConnectionMemberRemote? {
        val userId = getString("userId") ?: return null
        return ConnectionMemberRemote(
            connectionId = getString("connectionId") ?: fallbackConnectionId,
            userId = userId,
            role = getString("role") ?: "member",
            joinedAt = getLong("joinedAt") ?: 0L,
            status = getString("status")?.let { MemberStatus.valueOf(it) } ?: MemberStatus.ACTIVE
        )
    }
}
