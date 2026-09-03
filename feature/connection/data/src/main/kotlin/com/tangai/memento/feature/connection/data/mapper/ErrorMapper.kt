package com.tangai.memento.feature.connection.data.mapper

import com.tangai.memento.feature.connection.data.model.ConnectionDataError
import com.tangai.memento.feature.connection.data.model.ConnectionDataException
import com.tangai.memento.feature.connection.domain.model.ConnectionError
import com.tangai.memento.feature.connection.domain.model.ConnectionException

fun Throwable.toConnectionException(): ConnectionException {
    if (this is ConnectionException) return this
    
    val error = when (this) {
        is ConnectionDataException -> when (this.error) {
            ConnectionDataError.UserNotFound -> ConnectionError.UserNotFound
            ConnectionDataError.ConnectionNotFound -> ConnectionError.ConnectionNotFound
            ConnectionDataError.RequestNotFound -> ConnectionError.RequestNotFound
            ConnectionDataError.AlreadyConnected -> ConnectionError.AlreadyConnected
            ConnectionDataError.RequestAlreadyExists -> ConnectionError.RequestAlreadyExists
            ConnectionDataError.InvalidOperation -> ConnectionError.InvalidOperation
            ConnectionDataError.Network -> ConnectionError.Network
            is ConnectionDataError.Unknown -> ConnectionError.Unknown(this.error.message)
        }
        else -> ConnectionError.Unknown(message)
    }
    
    return ConnectionException(error)
}
