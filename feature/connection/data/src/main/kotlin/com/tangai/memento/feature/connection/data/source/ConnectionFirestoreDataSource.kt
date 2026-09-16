package com.tangai.memento.feature.connection.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.tangai.memento.feature.auth.data.source.util.awaitTask
import com.tangai.memento.feature.auth.data.mapper.toProfile
import com.tangai.memento.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/** Client reads Firestore directly; all invite/connection mutations go through Callable Functions. */
class ConnectionFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    suspend fun redeemDirectInvite(code: String): String {
        val data = functions.getHttpsCallable("redeemDirectInvite")
            .call(mapOf("code" to code)).awaitTask().data.asMap()
        return data["connectionId"] as? String ?: error("Redeem response is missing its connection.")
    }

    suspend fun getConnectionsForCurrentUser(): List<Connection> {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        return firestore.collection("connections")
            .whereArrayContains("memberIds", uid)
            .whereEqualTo("status", ConnectionStatus.ACTIVE.name)
            .orderBy("lastPostAt", Query.Direction.DESCENDING)
            .get().awaitTask().documents.map { doc ->
                val members = doc.reference.collection("members").get().awaitTask().documents.map { member ->
                    ConnectionMember(userId = member.id,
                        role = MemberRole.valueOf(requireNotNull(member.getString("role"))),
                        status = MemberStatus.valueOf(requireNotNull(member.getString("status"))),
                        joinedAt = member.getTimestamp("joinedAt")?.toDate()?.time ?: 0,
                        leftAt = member.getTimestamp("leftAt")?.toDate()?.time,
                        invitedBy = member.getString("invitedBy"), removedBy = member.getString("removedBy"))
                }
                Connection(id = doc.id, type = ConnectionType.valueOf(requireNotNull(doc.getString("type"))),
                    members = members,
                    status = ConnectionStatus.valueOf(requireNotNull(doc.getString("status"))),
                    name = doc.getString("name"), createdBy = requireNotNull(doc.getString("createdBy")),
                    ownerId = doc.getString("ownerId"), maxMembers = requireNotNull(doc.getLong("maxMembers")).toInt(),
                    directKey = doc.getString("directKey"), lastPostAt = doc.getTimestamp("lastPostAt")?.toDate()?.time,
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0,
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0,
                    schemaVersion = (doc.getLong("schemaVersion") ?: 1).toInt())
            }
    }

    fun observeCurrentUserConnectionChanges(): Flow<Unit> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("User is not signed in."))
            return@callbackFlow
        }
        val registration = firestore.collection("connections")
            .whereArrayContains("memberIds", uid)
            .whereEqualTo("status", ConnectionStatus.ACTIVE.name)
            .orderBy("lastPostAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> trySend(Unit)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val registration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> trySend(snapshot.toProfile())
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getUser(uid: String): User? =
        firestore.collection("users").document(uid).get().awaitTask().toProfile()

    suspend fun searchUsers(query: String): List<User> {
        val normalized = normalizeUsername(query)
        if (normalized.isBlank()) return emptyList()
        return firestore.collection("users").whereEqualTo("usernameNormalized", normalized)
            .limit(20).get().awaitTask().documents.mapNotNull { it.toProfile() }
    }

    private fun Any?.asMap(): Map<*, *> = this as? Map<*, *>
        ?: error("Unexpected response from invite service.")
}
