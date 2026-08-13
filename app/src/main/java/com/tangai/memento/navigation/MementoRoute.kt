package com.tangai.memento.navigation

sealed class MementoRoute(
    val route: String
) {
    data object Splash: MementoRoute("splash")
    data object Login: MementoRoute("login")
    data object Register: MementoRoute("register")
    data object Home: MementoRoute("home")
    data object Connection: MementoRoute("connection")
    data object Media: MementoRoute("media")
    data object History: MementoRoute("history")
}