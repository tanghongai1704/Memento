package com.tangai.memento.feature.auth.domain.usecase

import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.auth.domain.model.RegisterError
import com.tangai.memento.feature.auth.domain.model.RegisterException
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        val result = authRepository.signUp(email, password)
        return result.fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                val exception = if (throwable is RegisterException) {
                    throwable
                } else {
                    RegisterException(RegisterError.Unknown(throwable.message))
                }
                Result.failure(exception)
            }
        )
    }
}
