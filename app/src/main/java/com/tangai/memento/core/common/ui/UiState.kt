package com.tangai.memento.core.common.ui

sealed interface UiState<T> {
    data class Loading<T>(val value: Unit = Unit) : UiState<T>
    data class Empty<T>(val value: Unit = Unit) : UiState<T>
    data class Success<T>(val data: T) : UiState<T>
    data class Error<T>(val message: String) : UiState<T>
}