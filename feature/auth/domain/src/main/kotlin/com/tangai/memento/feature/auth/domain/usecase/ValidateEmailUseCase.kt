package com.tangai.memento.feature.auth.domain.usecase

import com.tangai.memento.feature.auth.domain.model.ValidationResult
import javax.inject.Inject

class ValidateEmailUseCase @Inject constructor() {
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    operator fun invoke(email: String): ValidationResult {
        if (email.isBlank()) return ValidationResult.Invalid("Email is required.")
        if (!emailRegex.matches(email)) return ValidationResult.Invalid("Invalid email format.")
        return ValidationResult.Valid
    }
}
