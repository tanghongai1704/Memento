package com.tangai.memento.navigation

sealed class MementoRoute(
    val route: String
) {
    data object Splash : MementoRoute("splash")
    data object Login : MementoRoute("login")
    data object Register : MementoRoute("register")
    data object ForgotPassword : MementoRoute("forgot_password")
    data object Home : MementoRoute("home")
    data object Connection : MementoRoute("connection")
    data object History : MementoRoute("history")
    data object CreatePost : MementoRoute("create_post")
    data object MediaPicker : MementoRoute("media_picker")
    data object MediaPreview : MementoRoute("media_preview")
}