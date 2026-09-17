package com.tangai.memento.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    suspend fun shouldNavigateToHome(): Boolean {
        if (!auth.isUserLoggedIn()) return false

        // Do not hold the splash screen on network work. The signed-in user can use the
        // locally cached profile while Firebase catches up in the background.
        viewModelScope.launch {
            auth.syncCurrentUserProfile()
        }
        return true
    }
}
