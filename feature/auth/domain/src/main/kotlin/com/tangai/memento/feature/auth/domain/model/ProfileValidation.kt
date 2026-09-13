package com.tangai.memento.feature.auth.domain.model

data class ProfileValidation(
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val bioError: String? = null
) {
    val isValid: Boolean
        get() = displayNameError == null && usernameError == null && bioError == null
}

object ProfileValidator {
    private val usernamePattern = Regex("^[A-Za-z0-9][A-Za-z0-9._]{1,29}$")

    fun validate(displayName: String, username: String, bio: String): ProfileValidation {
        val cleanDisplayName = displayName.trim()
        val cleanUsername = username.trim()
        return ProfileValidation(
            displayNameError = when {
                cleanDisplayName.isEmpty() -> "Display name is required."
                cleanDisplayName.length > 100 -> "Display name must be at most 100 characters."
                else -> null
            },
            usernameError = when {
                cleanUsername.length !in 2..30 -> "Username must be 2–30 characters."
                !usernamePattern.matches(cleanUsername) ->
                    "Use letters, numbers, dots or underscores; start with a letter or number."
                else -> null
            },
            bioError = if (bio.length > 500) "Bio must be at most 500 characters." else null
        )
    }
}
