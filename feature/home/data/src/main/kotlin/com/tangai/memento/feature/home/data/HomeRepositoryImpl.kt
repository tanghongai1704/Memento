package com.tangai.memento.feature.home.data

import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.domain.model.*
import com.tangai.memento.feature.home.domain.*
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(private val database: MementoDatabase,
    private val auth: FirebaseAuth,
    private val connections: com.tangai.memento.feature.connection.domain.ConnectionRepository) : HomeRepository {
    override suspend fun loadPosts(): Result<List<Post>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        database.postDao().loadPosts(uid).also { check(auth.currentUser?.uid == uid) }
    }
    override suspend fun loadConnections(): Result<List<Connection>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        connections.getCurrentUserConnections().getOrThrow()
        database.connectionMemberDao().getActiveMembershipsForUser(uid).mapNotNull { member ->
            database.connectionDao().getConnectionById(member.connectionId)
                ?.takeIf { it.status == "ACTIVE" }?.toDomain()
        }.sortedByDescending { it.lastPostAt }.also { check(auth.currentUser?.uid == uid) }
    }
    override fun getFilteredPosts(posts: List<Post>, filter: FeedFilter): List<Post> =
        posts.filter { filter is FeedFilter.All || (filter is FeedFilter.Connection && it.connectionId == filter.connectionId) }
            .sortedByDescending { it.createdAt ?: it.clientCreatedAt }
}
