package com.tangai.memento.feature.connection.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tangai.memento.feature.auth.data.source.util.awaitTask
import com.tangai.memento.feature.auth.data.mapper.toProfile
import com.tangai.memento.domain.model.*
import javax.inject.Inject

/** Read side only. Connection/member mutations belong to the future invite transaction. */
class ConnectionFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun getConnectionsForCurrentUser(): List<Connection> {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        return firestore.collection("connections")
            .whereArrayContains("memberIds", uid)
            .whereEqualTo("status", "ACTIVE")
            .orderBy("lastPostAt", Query.Direction.DESCENDING)
            .get().awaitTask().documents.map { doc ->
                val members = doc.reference.collection("members").get().awaitTask().documents.map { member ->
                    ConnectionMember(userId = member.id,
                        role = requireNotNull(member.getString("role")),
                        status = MemberStatus.valueOf(requireNotNull(member.getString("status"))),
                        joinedAt = member.getTimestamp("joinedAt")?.toDate()?.time ?: 0,
                        leftAt = member.getTimestamp("leftAt")?.toDate()?.time,
                        invitedBy = member.getString("invitedBy"), removedBy = member.getString("removedBy"))
                }
                Connection(id = doc.id, type = ConnectionType.valueOf(requireNotNull(doc.getString("type"))),
                    members = members, status = ConnectionStatus.ACTIVE,
                    name = doc.getString("name"), createdBy = requireNotNull(doc.getString("createdBy")),
                    ownerId = doc.getString("ownerId"), maxMembers = requireNotNull(doc.getLong("maxMembers")).toInt(),
                    directKey = doc.getString("directKey"), lastPostAt = doc.getTimestamp("lastPostAt")?.toDate()?.time,
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0,
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0,
                    schemaVersion = (doc.getLong("schemaVersion") ?: 1).toInt())
            }
    }

    suspend fun getUser(uid: String): User? =
        firestore.collection("users").document(uid).get().awaitTask().toProfile()

    suspend fun searchUsers(query: String): List<User> {
        val normalized = normalizeUsername(query)
        if (normalized.isBlank()) return emptyList()
        return firestore.collection("users").whereEqualTo("usernameNormalized", normalized)
            .limit(20).get().awaitTask().documents.mapNotNull { it.toProfile() }
    }
}
