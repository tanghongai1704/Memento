package com.tangai.memento.feature.history.domain

import com.tangai.memento.domain.model.Post

interface HistoryRepository {
    suspend fun loadHistory(): Result<List<Post>>
}
