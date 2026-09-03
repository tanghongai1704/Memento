package com.tangai.memento.feature.auth.presentation.viewmodel

sealed interface LogoutEffect {
    data object NavigateToLogin : LogoutEffect
}
