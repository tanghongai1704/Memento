package com.tangai.memento.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.feature.auth.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    private val _effect = MutableSharedFlow<LogoutEffect>()
    val effect: SharedFlow<LogoutEffect> = _effect.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effect.emit(LogoutEffect.NavigateToLogin)
        }
    }
}
