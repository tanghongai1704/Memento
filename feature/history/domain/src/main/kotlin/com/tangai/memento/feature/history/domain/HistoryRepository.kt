package com.tangai.memento.feature.history.domain

interface HistoryRepository {
    suspend fun loadHistory(): Result<List<String>>
}
