package com.tangai.memento.feature.auth.domain.usecase

import com.tangai.memento.feature.auth.domain.model.RegisterValidationResult
import com.tangai.memento.feature.auth.domain.model.ValidationResult
import javax.inject.Inject

class ValidateRegisterInputUseCase @Inject constructor(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String
    ): RegisterValidationResult {
        val emailResult = validateEmailUseCase(email)
        val passwordResult = validatePasswordUseCase(password)
        val confirmResult = when {
            confirmPassword.isBlank() -> ValidationResult.Invalid("Confirm password is required.")
            confirmPassword != password -> ValidationResult.Invalid("Confirm password does not match.")
            else -> ValidationResult.Valid
        }
        return RegisterValidationResult(
            email = emailResult,
            password = passwordResult,
            confirmPassword = confirmResult
        )
    }
}
