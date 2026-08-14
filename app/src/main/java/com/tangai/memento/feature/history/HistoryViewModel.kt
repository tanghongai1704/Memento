package com.tangai.memento.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.core.common.ui.UiState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<String>>>(
        UiState.Loading()
    )
    val uiState: StateFlow<UiState<List<String>>> = _uiState.asStateFlow()

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading()

            delay(1000.milliseconds) // Simulate a network or database delay

            val fakeData = listOf(
                "Moment 1 - Coffee with friend",
                "Moment 2 - Sunset at the beach",
                "Moment 3 - First Android app launch"
            )

            _uiState.value = UiState.Success(fakeData)
        }
    }

    fun showEmptyState() {
        _uiState.value = UiState.Empty()
    }

    fun showErrorState() {
        _uiState.value = UiState.Error("Failed to load history")
    }
}