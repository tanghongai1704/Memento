package com.tangai.memento.feature.connection.data.model

sealed interface ConnectionDataError {
    data object UserNotFound : ConnectionDataError
    data object ConnectionNotFound : ConnectionDataError
    data object RequestNotFound : ConnectionDataError
    data object AlreadyConnected : ConnectionDataError
    data object RequestAlreadyExists : ConnectionDataError
    data object InvalidOperation : ConnectionDataError
    data object Network : ConnectionDataError
    data class Unknown(val message: String? = null) : ConnectionDataError
}

class ConnectionDataException(
    val error: ConnectionDataError
) : Exception(
    when (error) {
        ConnectionDataError.UserNotFound -> "User not found."
        ConnectionDataError.ConnectionNotFound -> "Connection not found."
        ConnectionDataError.RequestNotFound -> "Request not found."
        ConnectionDataError.AlreadyConnected -> "You are already connected with this user."
        ConnectionDataError.RequestAlreadyExists -> "A connection request already exists."
        ConnectionDataError.InvalidOperation -> "Invalid operation."
        ConnectionDataError.Network -> "Network error. Please try again."
        is ConnectionDataError.Unknown -> error.message ?: "Operation failed."
    }
)
