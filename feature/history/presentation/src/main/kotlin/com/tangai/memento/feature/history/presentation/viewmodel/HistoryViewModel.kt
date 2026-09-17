package com.tangai.memento.feature.history.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.Post
import com.tangai.memento.feature.history.domain.HistoryRepository
import com.tangai.memento.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Post>>>(UiState.Loading())
    val uiState: StateFlow<UiState<List<Post>>> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading()

            val result = historyRepository.loadHistory()
            _uiState.value = if (result.isSuccess) {
                val history = result.getOrDefault(emptyList())
                if (history.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(history)
                }
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to load history")
            }
        }
    }
}
