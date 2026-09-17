package com.tangai.memento.feature.history.data

import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.feature.history.domain.HistoryRepository
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(private val database: MementoDatabase,
    private val auth: FirebaseAuth) : HistoryRepository {
    override suspend fun loadHistory() = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        database.postDao().loadPosts(uid)
            .also { check(auth.currentUser?.uid == uid) }
    }
}
