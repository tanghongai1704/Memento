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
) : Exception()
