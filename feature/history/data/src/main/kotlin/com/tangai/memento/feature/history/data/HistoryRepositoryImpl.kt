package com.tangai.memento.feature.history.data

import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.feature.history.domain.HistoryEntry
import com.tangai.memento.feature.history.domain.HistoryRepository
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(private val database: MementoDatabase,
    private val auth: FirebaseAuth) : HistoryRepository {
    override suspend fun loadHistory() = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        val posts = database.postDao().loadPosts(uid)
        val usersById = database.userDao().getAllUsers()
            .associate { user -> user.id to user.toDomain() }
        val connectionUsers = posts.map { it.connectionId }.distinct().associateWith { connectionId ->
            database.connectionMemberDao().getMembersByConnectionId(connectionId)
                .firstOrNull { it.userId != uid }
                ?.userId
                ?.let(usersById::get)
        }
        check(auth.currentUser?.uid == uid)
        posts.map { post ->
            HistoryEntry(
                post = post,
                author = usersById[post.authorId],
                connectionUser = connectionUsers[post.connectionId],
                isOutgoing = post.authorId == uid
            )
        }
    }
}
