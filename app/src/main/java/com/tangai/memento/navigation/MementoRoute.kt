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
    data object Profile : MementoRoute("profile")
    data object CreatePost : MementoRoute("create_post")
}
