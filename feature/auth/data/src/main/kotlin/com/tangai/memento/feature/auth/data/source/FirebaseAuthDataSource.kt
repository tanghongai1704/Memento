package com.tangai.memento.feature.auth.data.source

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.tangai.memento.feature.auth.data.model.AuthDataError
import com.tangai.memento.feature.auth.data.model.AuthDataException
import com.tangai.memento.network.awaitFirebaseTask
import com.tangai.memento.network.runSuspendCatching
import javax.inject.Inject

class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    private companion object {
        const val TAG = "FirebaseAuthDataSource"
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> {
        val normalizedEmail = email.trim()
        return runSuspendCatching {
            firebaseAuth
                .signInWithEmailAndPassword(normalizedEmail, password)
                .awaitFirebaseTask()
            Unit
        }.mapUnitError()
    }

    suspend fun signUp(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        val normalizedEmail = email.trim()
        return runSuspendCatching {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(normalizedEmail, password)
                .awaitFirebaseTask()
            authResult.user ?: throw AuthDataException(AuthDataError.Unknown("User is null after sign-up."))
        }.mapError()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return runSuspendCatching {
            firebaseAuth
                .sendPasswordResetEmail(email.trim())
                .awaitFirebaseTask()
            Unit
        }.mapUnitError()
    }

    fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun logout() {
        firebaseAuth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    private fun Result<FirebaseUser>.mapError(): Result<FirebaseUser> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                logFirebaseError("signUp", throwable)
                val mappedError = when (throwable) {
                    is AuthDataException -> throwable.error
                    is FirebaseAuthUserCollisionException -> AuthDataError.EmailAlreadyInUse
                    is FirebaseAuthWeakPasswordException -> AuthDataError.WeakPassword
                    is FirebaseNetworkException -> AuthDataError.Network
                    is FirebaseAuthException -> throwable.toAuthDataError()
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
                logFirebaseError("login", throwable)
                val mappedError = when (throwable) {
                    is FirebaseNetworkException -> AuthDataError.Network
                    is FirebaseTooManyRequestsException -> AuthDataError.Unknown("Too many attempts. Please try again later.")
                    is FirebaseAuthException -> throwable.toAuthDataError()
                    else -> AuthDataError.Unknown(throwable.message)
                }
                Result.failure(AuthDataException(mappedError))
            }
        )
    }

    private fun FirebaseAuthException.toAuthDataError(): AuthDataError {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> AuthDataError.InvalidEmail
            "ERROR_USER_NOT_FOUND" -> AuthDataError.InvalidCredentials
            "ERROR_WRONG_PASSWORD" -> AuthDataError.InvalidCredentials
            "ERROR_INVALID_CREDENTIAL" -> AuthDataError.InvalidCredentials
            "ERROR_INVALID_LOGIN_CREDENTIALS" -> AuthDataError.InvalidCredentials
            "ERROR_USER_DISABLED" -> AuthDataError.Unknown("This Firebase account has been disabled.")
            "ERROR_OPERATION_NOT_ALLOWED" -> AuthDataError.Unknown("Email/password sign-in is not enabled in Firebase Authentication.")
            "ERROR_TOO_MANY_REQUESTS" -> AuthDataError.Unknown("Too many attempts. Please try again later.")
            else -> AuthDataError.Unknown("Login failed. Please try again.")
        }
    }

    private fun logFirebaseError(action: String, throwable: Throwable) {
        val details = if (throwable is FirebaseAuthException) {
            "code=${throwable.errorCode}, message=${throwable.message}"
        } else {
            "message=${throwable.message}"
        }
        Log.e(TAG, "Firebase auth $action failed: $details", throwable)
    }
}
