package com.tangai.memento.feature.auth.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.tangai.memento.database.dao.UserDao
import com.tangai.memento.database.model.toEntity
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.auth.data.mapper.toDomainUser
import com.tangai.memento.feature.auth.data.model.AuthDataError
import com.tangai.memento.feature.auth.data.model.AuthDataException
import com.tangai.memento.feature.auth.data.source.FirebaseAuthDataSource
import com.tangai.memento.feature.auth.data.source.util.awaitTask
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.auth.domain.model.RegisterError
import com.tangai.memento.feature.auth.domain.model.RegisterException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun login(account: String, password: String): Result<Unit> {
        return firebaseAuthDataSource.login(account, password).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it.toRegisterException()) }
        )
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return firebaseAuthDataSource.signUp(email, password).fold(
            onSuccess = { firebaseUser ->
                runCatching {
                    val user = firebaseUser.toDomainUser()
                    createUserProfile(user)
                    userDao.upsertUser(user.toEntity())
                    user
                }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(it.toRegisterException()) }
                )
            },
            onFailure = { Result.failure(it.toRegisterException()) }
        )
    }

    override suspend fun syncUsers(): Result<Unit> = runCatching {
        val snapshot = firestore
            .collection("users")
            .get()
            .awaitTask()

        snapshot.documents.mapNotNull { document ->
            val uid = document.getString("uid") ?: document.id
            val email = document.getString("email")
                ?: document.getString("account")
                ?: document.getString("mail")
                ?: ""
            val username = document.getString("username")
                ?: document.getString("displayName")
                ?: document.getString("name")
                ?: ""
            if (uid.isBlank() || email.isBlank()) return@mapNotNull null
            User(id = uid, username = username.ifBlank { email }, email = email)
        }.forEach { user ->
            userDao.upsertUser(user.toEntity())
        }
    }

    override fun logout() {
        firebaseAuthDataSource.logout()
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuthDataSource.isUserLoggedIn()
    }

    private suspend fun createUserProfile(user: User) {
        val payload = mapOf(
            "uid" to user.id,
            "email" to user.email,
            "username" to user.username,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore
            .collection("users")
            .document(user.id)
            .set(payload)
            .awaitTask()
    }

    private fun Throwable.toRegisterException(): RegisterException {
        if (this is RegisterException) return this
        val error = when (this) {
            is AuthDataException -> when (error) {
                AuthDataError.EmailAlreadyInUse -> RegisterError.EmailAlreadyInUse
                AuthDataError.WeakPassword -> RegisterError.WeakPassword
                AuthDataError.InvalidEmail -> RegisterError.InvalidEmail
                AuthDataError.InvalidCredentials -> RegisterError.Unknown(message ?: "Invalid email or password.")
                AuthDataError.Network -> RegisterError.Network
                is AuthDataError.Unknown -> RegisterError.Unknown(error.message)
            }
            else -> RegisterError.Unknown(message)
        }
        return RegisterException(error)
    }
}
