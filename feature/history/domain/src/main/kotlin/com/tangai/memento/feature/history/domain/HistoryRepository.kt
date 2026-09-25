package com.tangai.memento.feature.history.domain

import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User

data class HistoryEntry(
    val post: Post,
    val author: User?,
    val connectionUser: User?,
    val isOutgoing: Boolean
)

interface HistoryRepository {
    suspend fun loadHistory(): Result<List<HistoryEntry>>
}
