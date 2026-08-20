package com.tangai.memento.feature.auth.domain.model

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}

data class RegisterValidationResult(
    val email: ValidationResult,
    val password: ValidationResult,
    val confirmPassword: ValidationResult
) {
    val isValid: Boolean
        get() = email is ValidationResult.Valid &&
                password is ValidationResult.Valid &&
                confirmPassword is ValidationResult.Valid
}
