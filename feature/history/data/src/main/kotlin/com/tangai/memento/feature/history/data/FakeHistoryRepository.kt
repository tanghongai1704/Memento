package com.tangai.memento.feature.history.data

import com.tangai.memento.feature.history.domain.HistoryRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject

class FakeHistoryRepository @Inject constructor() : HistoryRepository {
    override suspend fun loadHistory(): Result<List<String>> {
        delay(1000.milliseconds)
        val fakeData = listOf(
            "Moment 1 - Coffee with friend",
            "Moment 2 - Sunset at the beach",
            "Moment 3 - First Android app launch"
        )
        return Result.success(fakeData)
    }
}
