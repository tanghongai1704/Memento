package com.tangai.memento.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tangai.memento.ui.theme.MementoTheme
import com.tangai.memento.feature.auth.SplashScreen
import com.tangai.memento.feature.auth.LoginScreen
import com.tangai.memento.feature.auth.RegisterScreen
import com.tangai.memento.feature.home.HomeScreen
import com.tangai.memento.feature.history.HistoryScreen
import com.tangai.memento.feature.media.MediaScreen
import com.tangai.memento.feature.connection.ConnectionScreen

@Composable
fun MementoNavGraph() {
    val navController = rememberNavController()

    MementoTheme {
        NavHost(
            navController = navController,
            startDestination = MementoRoute.Splash.route
        ) {
            composable(MementoRoute.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(MementoRoute.Login.route) {
                            popUpTo(MementoRoute.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(MementoRoute.Login.route) {
                LoginScreen(
                    onNavigateToHome = {
                        navController.navigate(MementoRoute.Home.route) {
                            popUpTo(MementoRoute.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(MementoRoute.Register.route)
                    }
                )
            }

            composable(MementoRoute.Register.route) {
                RegisterScreen(
                    onNavigateBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(MementoRoute.Home.route) {
                HomeScreen(
                    onNavigateToConnection = {
                        navController.navigate(MementoRoute.Connection.route)
                    },
                    onNavigateToMedia = {
                        navController.navigate(MementoRoute.Media.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(MementoRoute.History.route)
                    }
                )
            }

            composable(MementoRoute.Connection.route) {
                ConnectionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(MementoRoute.Media.route) {
                MediaScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(MementoRoute.History.route) {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
