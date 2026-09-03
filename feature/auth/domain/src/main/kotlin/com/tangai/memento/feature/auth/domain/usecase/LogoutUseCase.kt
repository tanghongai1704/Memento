package com.tangai.memento.feature.auth.domain.usecase

import com.tangai.memento.feature.auth.domain.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke() {
        authRepository.logout()
    }
}
