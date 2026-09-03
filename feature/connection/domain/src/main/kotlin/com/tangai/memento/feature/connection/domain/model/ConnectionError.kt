package com.tangai.memento.feature.connection.domain.model

sealed interface ConnectionError {
    val message: String

    data object UserNotFound : ConnectionError {
        override val message: String = "User not found."
    }

    data object ConnectionNotFound : ConnectionError {
        override val message: String = "Connection not found."
    }

    data object RequestNotFound : ConnectionError {
        override val message: String = "Connection request not found."
    }

    data object AlreadyConnected : ConnectionError {
        override val message: String = "You are already connected with this user."
    }

    data object RequestAlreadyExists : ConnectionError {
        override val message: String = "A connection request already exists."
    }

    data object InvalidOperation : ConnectionError {
        override val message: String = "Invalid operation. Please try again."
    }

    data object Network : ConnectionError {
        override val message: String = "Network error. Please check your connection and try again."
    }

    data class Unknown(private val detail: String? = null) : ConnectionError {
        override val message: String = detail ?: "Something went wrong. Please try again."
    }
}

class ConnectionException(
    val error: ConnectionError
) : Exception(error.message)
