package com.tangai.memento.feature.auth.domain.usecase

import com.tangai.memento.feature.auth.domain.model.ValidationResult
import javax.inject.Inject

class ValidatePasswordUseCase @Inject constructor() {
    private val hasLetterRegex = ".*[A-Za-z].*".toRegex()
    private val hasDigitRegex = ".*\\d.*".toRegex()

    operator fun invoke(password: String): ValidationResult {
        if (password.isBlank()) return ValidationResult.Invalid("Password is required.")
        if (password.length < 8) return ValidationResult.Invalid("Password must be at least 8 characters.")
        if (!hasLetterRegex.matches(password) || !hasDigitRegex.matches(password)) {
            return ValidationResult.Invalid("Password must include both letters and numbers.")
        }
        return ValidationResult.Valid
    }
}
