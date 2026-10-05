package com.tangai.memento.navigation

import android.net.Uri

sealed class MementoRoute(
    val route: String
) {
    data object Splash : MementoRoute("splash")
    data object Login : MementoRoute("login")
    data object Register : MementoRoute("register")
    data object ForgotPassword : MementoRoute("forgot_password")
    data object Home : MementoRoute("home")
    data object Connection : MementoRoute("connection")
    data object Profile : MementoRoute("profile")
    data object CreatePost : MementoRoute("create_post?$RECIPIENT_ID_ARG={$RECIPIENT_ID_ARG}") {
        fun destination(recipientId: String?): String = recipientId
            ?.let { "create_post?$RECIPIENT_ID_ARG=${Uri.encode(it)}" }
            ?: "create_post"
    }

    companion object {
        const val RECIPIENT_ID_ARG = "recipientId"
    }
}
