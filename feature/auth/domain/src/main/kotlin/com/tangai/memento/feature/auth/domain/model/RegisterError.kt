package com.tangai.memento.feature.auth.domain.model

sealed interface RegisterError {
    val message: String

    data object EmailAlreadyInUse : RegisterError {
        override val message: String = "This email is already registered."
    }

    data object WeakPassword : RegisterError {
        override val message: String = "Password is too weak. Use at least 8 characters with letters and numbers."
    }

    data object InvalidEmail : RegisterError {
        override val message: String = "Invalid email format."
    }

    data object Network : RegisterError {
        override val message: String = "Network error. Please check your connection and try again."
    }

    data class Unknown(private val detail: String? = null) : RegisterError {
        override val message: String = detail ?: "Something went wrong. Please try again."
    }
}

class RegisterException(
    val error: RegisterError
) : Exception(error.message)
