package com.tangai.memento.feature.auth.data.source

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.tangai.memento.feature.auth.data.model.AuthDataError
import com.tangai.memento.feature.auth.data.model.AuthDataException
import com.tangai.memento.feature.auth.data.source.util.awaitTask
import javax.inject.Inject

class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> {
        return runCatching {
            firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .awaitTask()
            Unit
        }.mapUnitError()
    }

    suspend fun signUp(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return runCatching {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .awaitTask()
            authResult.user ?: throw AuthDataException(AuthDataError.Unknown("User is null after sign-up."))
        }.mapError()
    }

    private fun Result<FirebaseUser>.mapError(): Result<FirebaseUser> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                val mappedError = when (throwable) {
                    is AuthDataException -> throwable.error
                    is FirebaseAuthUserCollisionException -> AuthDataError.EmailAlreadyInUse
                    is FirebaseAuthWeakPasswordException -> AuthDataError.WeakPassword
                    is FirebaseAuthInvalidCredentialsException -> AuthDataError.InvalidEmail
                    is FirebaseNetworkException -> AuthDataError.Network
                    else -> AuthDataError.Unknown(throwable.message)
                }
                Result.failure(AuthDataException(mappedError))
            }
        )
    }

    private fun Result<Unit>.mapUnitError(): Result<Unit> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                val mappedError = when (throwable) {
                    is FirebaseAuthInvalidCredentialsException -> AuthDataError.InvalidCredentials
                    is FirebaseNetworkException -> AuthDataError.Network
                    else -> AuthDataError.Unknown(throwable.message)
                }
                Result.failure(AuthDataException(mappedError))
            }
        )
    }
}
