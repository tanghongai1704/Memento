package com.tangai.memento.feature.auth.data.model

sealed interface AuthDataError {
    data object EmailAlreadyInUse : AuthDataError
    data object WeakPassword : AuthDataError
    data object InvalidEmail : AuthDataError
    data object InvalidCredentials : AuthDataError
    data object Network : AuthDataError
    data class Unknown(val message: String? = null) : AuthDataError
}

class AuthDataException(
    val error: AuthDataError
) : Exception(
    when (error) {
        AuthDataError.EmailAlreadyInUse -> "This email is already registered."
        AuthDataError.WeakPassword -> "Password is too weak."
        AuthDataError.InvalidEmail -> "Invalid email format."
        AuthDataError.InvalidCredentials -> "Invalid email or password."
        AuthDataError.Network -> "Network error. Please try again."
        is AuthDataError.Unknown -> error.message ?: "Authentication failed."
    }
)
