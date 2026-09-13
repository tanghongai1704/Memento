package com.tangai.memento.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.tangai.memento.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    suspend fun shouldNavigateToHome(): Boolean {
        if (!auth.isUserLoggedIn()) return false
        return auth.syncCurrentUserProfile().isSuccess.also { ready ->
            if (!ready) auth.logout()
        }
    }
}
