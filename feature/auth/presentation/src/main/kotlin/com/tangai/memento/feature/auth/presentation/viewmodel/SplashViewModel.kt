package com.tangai.memento.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.tangai.memento.feature.auth.domain.usecase.IsUserLoggedInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase
) : ViewModel() {
    fun shouldNavigateToHome(): Boolean = isUserLoggedInUseCase()
}
