package com.tangai.memento.feature.auth.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctions
import com.tangai.memento.database.dao.UserDao
import com.tangai.memento.database.model.toEntity
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.feature.auth.data.mapper.toProfile
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.auth.data.mapper.toDomainUser
import com.tangai.memento.feature.auth.data.model.AuthDataError
import com.tangai.memento.feature.auth.data.model.AuthDataException
import com.tangai.memento.feature.auth.data.source.FirebaseAuthDataSource
import com.tangai.memento.feature.auth.data.source.util.awaitTask
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.auth.domain.model.RegisterError
import com.tangai.memento.feature.auth.domain.model.RegisterException
import com.tangai.memento.feature.auth.domain.model.ProfileValidator
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun login(account: String, password: String): Result<Unit> {
        return firebaseAuthDataSource.login(account, password).fold(
            onSuccess = { syncCurrentUserProfile().onFailure { firebaseAuthDataSource.logout() } },
            onFailure = { Result.failure(it.toRegisterException()) }
        )
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return firebaseAuthDataSource.signUp(email, password).fold(
            onSuccess = { firebaseUser ->
                runCatching {
                    syncCurrentUserProfile().getOrThrow()
                    userDao.getUserById(firebaseUser.uid)!!.toDomain()

                }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = {
                        firebaseAuthDataSource.logout()
                        Result.failure(RegisterException(RegisterError.Unknown(
                            "Account created, but profile setup failed. Sign in again to retry profile setup.")))
                    }
                )
            },
            onFailure = { Result.failure(it.toRegisterException()) }
        )
    }

    override suspend fun syncCurrentUserProfile(): Result<Unit> = runCatching {
        val authUser = firebaseAuthDataSource.currentUser()
            ?: error("User is not signed in.")
        val fallback = authUser.toDomainUser()
        val ref = firestore.collection("users").document(authUser.uid)
        // Idempotent recovery after Auth succeeds but the profile write fails.
        firestore.runTransaction { tx ->
            val existing = tx.get(ref)
            val username = existing.getString("username")?.takeIf { it.isNotBlank() } ?: fallback.username
            val normalized = com.tangai.memento.domain.model.normalizeUsername(username)
            val fields = setOf("displayName", "username", "usernameNormalized", "avatarPath", "bio",
                "createdAt", "updatedAt", "schemaVersion")
            if (!existing.exists() || existing.data?.keys != fields ||
                existing.getString("usernameNormalized") != normalized) {
                tx.set(ref, mapOf(
                    "displayName" to (existing.getString("displayName") ?: username),
                    "username" to username,
                    "usernameNormalized" to normalized,
                    "avatarPath" to existing.getString("avatarPath"),
                    "bio" to existing.getString("bio"),
                    "createdAt" to (existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "schemaVersion" to 1
                ))
            }
        }.awaitTask()
        val profile = ref.get().awaitTask().toProfile() ?: error("Invalid user profile.")
        check(firebaseAuthDataSource.currentUser()?.uid == authUser.uid) { "Account changed during sync." }
        userDao.upsertUser(profile.toEntity())
        getCurrentUserInviteCode().getOrThrow()
        Unit
    }.recoverCatching { error ->
        val uid = firebaseAuthDataSource.currentUser()?.uid ?: throw error
        if (!error.isOfflineFailure() || userDao.getUserById(uid) == null) throw error
    }

    override suspend fun getCurrentUserProfile(): Result<User> = runCatching {
        val authUser = firebaseAuthDataSource.currentUser()
            ?: error("User is not signed in.")
        val profile = firestore.collection("users")
            .document(authUser.uid)
            .get()
            .awaitTask()
            .toProfile()
            ?: error("User profile is missing.")
        check(firebaseAuthDataSource.currentUser()?.uid == authUser.uid) {
            "Account changed while loading profile."
        }
        userDao.upsertUser(profile.toEntity())
        profile
    }

    override suspend fun getCachedCurrentUserProfile(): Result<User> = runCatching {
        val uid = firebaseAuthDataSource.currentUser()?.uid ?: error("User is not signed in.")
        userDao.getUserById(uid)?.toDomain() ?: error("No cached profile is available.")
    }

    override suspend fun getCurrentUserInviteCode(): Result<String> = runCatching {
        val uid = firebaseAuthDataSource.currentUser()?.uid ?: error("User is not signed in.")
        val data = functions.getHttpsCallable("getMyInviteCode")
            .call().awaitTask().data as? Map<*, *> ?: error("Unexpected invite code response.")
        check(firebaseAuthDataSource.currentUser()?.uid == uid) { "Account changed while loading invite code." }
        data["code"] as? String ?: error("Invite code response is missing its code.")
    }

    override suspend fun updateCurrentUserProfile(
        displayName: String,
        username: String,
        bio: String?
    ): Result<User> = runCatching {
        val cleanDisplayName = displayName.trim()
        val cleanUsername = username.trim()
        val cleanBio = bio?.trim()?.takeIf { it.isNotEmpty() }
        val validation = ProfileValidator.validate(
            displayName = cleanDisplayName,
            username = cleanUsername,
            bio = cleanBio.orEmpty()
        )
        require(validation.isValid) {
            validation.displayNameError ?: validation.usernameError ?: validation.bioError
                ?: "Invalid profile."
        }

        val authUser = firebaseAuthDataSource.currentUser()
            ?: error("User is not signed in.")
        val ref = firestore.collection("users").document(authUser.uid)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(ref)
            require(existing.exists()) { "User profile is missing." }
            transaction.set(ref, mapOf(
                "displayName" to cleanDisplayName,
                "username" to cleanUsername,
                "usernameNormalized" to com.tangai.memento.domain.model.normalizeUsername(cleanUsername),
                "avatarPath" to existing.getString("avatarPath"),
                "bio" to cleanBio,
                "createdAt" to requireNotNull(existing.getTimestamp("createdAt")),
                "updatedAt" to FieldValue.serverTimestamp(),
                "schemaVersion" to 1
            ))
        }.awaitTask()
        check(firebaseAuthDataSource.currentUser()?.uid == authUser.uid) {
            "Account changed while saving profile."
        }
        getCurrentUserProfile().getOrThrow()
        userDao.getUserById(authUser.uid)?.toDomain()
            ?: error("Saved profile is missing after update.")
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        firebaseAuthDataSource.sendPasswordResetEmail(email)

    override fun currentUserEmail(): String? = firebaseAuthDataSource.currentUser()?.email

    override fun logout() {
        firebaseAuthDataSource.logout()
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuthDataSource.isUserLoggedIn()
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

    private fun Throwable.isOfflineFailure(): Boolean = when (this) {
        is FirebaseNetworkException -> true
        is FirebaseFirestoreException -> code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        is FirebaseFunctionsException -> code == FirebaseFunctionsException.Code.UNAVAILABLE ||
            code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED
        else -> cause?.takeIf { it !== this }?.isOfflineFailure() == true
    }
}
