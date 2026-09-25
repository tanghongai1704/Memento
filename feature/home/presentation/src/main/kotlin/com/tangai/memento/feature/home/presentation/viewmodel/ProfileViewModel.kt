package com.tangai.memento.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.auth.domain.model.ProfileValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val email: String = "",
    val inviteCode: String = "",
    val inviteCodeError: String? = null,
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val bioError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = true,
    val isLoadingInviteCode: Boolean = false,
    val isSaving: Boolean = false
) {
    val hasChanges: Boolean
        get() = user?.let {
            displayName.trim() != it.displayName ||
                username.trim() != it.username ||
                bio.trim().ifEmpty { null } != it.bio
        } == true
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    private val state = MutableStateFlow(ProfileUiState())
    val uiState = state.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, errorMessage = null)
            val profile = repository.getCurrentUserProfile().getOrElse { error ->
                state.value = state.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Could not load profile."
                )
                return@launch
            }
            showUser(profile)
            loadInviteCode()
        }
    }

    fun retryInviteCode() {
        viewModelScope.launch { loadInviteCode() }
    }

    fun onDisplayNameChanged(value: String) {
        state.value = state.value.copy(displayName = value, displayNameError = null,
            errorMessage = null, successMessage = null)
    }

    fun onUsernameChanged(value: String) {
        state.value = state.value.copy(username = value, usernameError = null,
            errorMessage = null, successMessage = null)
    }

    fun onBioChanged(value: String) {
        state.value = state.value.copy(bio = value, bioError = null,
            errorMessage = null, successMessage = null)
    }

    fun discardChanges() {
        val current = state.value
        val user = current.user ?: return
        state.value = current.copy(
            displayName = user.displayName,
            username = user.username,
            bio = user.bio.orEmpty(),
            displayNameError = null,
            usernameError = null,
            bioError = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun saveProfile() {
        val current = state.value
        val validation = ProfileValidator.validate(current.displayName, current.username, current.bio)
        if (!validation.isValid) {
            state.value = current.copy(
                displayNameError = validation.displayNameError,
                usernameError = validation.usernameError,
                bioError = validation.bioError
            )
            return
        }
        viewModelScope.launch {
            state.value = state.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            repository.updateCurrentUserProfile(
                displayName = current.displayName,
                username = current.username,
                bio = current.bio
            ).fold(
                onSuccess = {
                    showUser(it, successMessage = "Profile updated.", inviteCode = current.inviteCode)
                },
                onFailure = { error ->
                    state.value = state.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Could not update profile."
                    )
                }
            )
        }
    }

    private suspend fun loadInviteCode() {
        state.value = state.value.copy(
            isLoadingInviteCode = true,
            inviteCodeError = null
        )
        repository.getCurrentUserInviteCode().fold(
            onSuccess = { inviteCode ->
                state.value = state.value.copy(
                    inviteCode = inviteCode,
                    inviteCodeError = null,
                    isLoadingInviteCode = false
                )
            },
            onFailure = {
                state.value = state.value.copy(
                    inviteCodeError = "Couldn’t load your invite code. Please try again.",
                    isLoadingInviteCode = false
                )
            }
        )
    }

    private fun showUser(user: User, successMessage: String? = null, inviteCode: String = state.value.inviteCode) {
        val current = state.value
        state.value = ProfileUiState(
            user = user,
            email = repository.currentUserEmail().orEmpty(),
            inviteCode = inviteCode,
            inviteCodeError = current.inviteCodeError,
            displayName = user.displayName,
            username = user.username,
            bio = user.bio.orEmpty(),
            successMessage = successMessage,
            isLoadingInviteCode = current.isLoadingInviteCode,
            isLoading = false
        )
    }
}
