package com.tangai.memento.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tangai.memento.core.designsystem.theme.MementoTheme
import com.tangai.memento.feature.auth.presentation.ui.ForgotPasswordScreen
import com.tangai.memento.feature.auth.presentation.ui.LoginScreen
import com.tangai.memento.feature.auth.presentation.ui.RegisterScreen
import com.tangai.memento.feature.auth.presentation.ui.SplashScreen
import com.tangai.memento.feature.connection.presentation.ui.ConnectionScreen
import com.tangai.memento.feature.history.presentation.ui.HistoryScreen
import com.tangai.memento.feature.home.presentation.ui.HomeScreen
import com.tangai.memento.feature.post.presentation.ui.CreatePostScreen
import com.tangai.memento.feature.post.presentation.ui.MediaPickerScreen
import com.tangai.memento.feature.post.presentation.ui.MediaPreviewScreen

@Composable
fun MementoNavGraph() {
    val navController = rememberNavController()

    MementoTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MementoRoute.Splash.route,
                modifier = androidx.compose.ui.Modifier.padding(innerPadding)
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
                        },
                        onNavigateToForgotPassword = {
                            navController.navigate(MementoRoute.ForgotPassword.route)
                        }
                    )
                }

                composable(MementoRoute.Register.route) {
                    RegisterScreen(
                        onNavigateToHome = {
                            navController.navigate(MementoRoute.Home.route) {
                                popUpTo(MementoRoute.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(MementoRoute.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(MementoRoute.Home.route) {
                    HomeScreen(
                        onNavigateToConnection = {
                            navController.navigate(MementoRoute.Connection.route)
                        },
                        onNavigateToHistory = {
                            navController.navigate(MementoRoute.History.route)
                        },
                        onNavigateToCreatePost = {
                            navController.navigate(MementoRoute.CreatePost.route)
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

                composable(MementoRoute.History.route) {
                    HistoryScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(MementoRoute.CreatePost.route) {
                    CreatePostScreen(
                        onNavigateToMediaPicker = {
                            navController.navigate(MementoRoute.MediaPicker.route)
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(MementoRoute.MediaPicker.route) {
                    MediaPickerScreen(
                        onNavigateToPreview = {
                            navController.navigate(MementoRoute.MediaPreview.route)
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(MementoRoute.MediaPreview.route) {
                    MediaPreviewScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToHome = {
                            navController.navigate(MementoRoute.Home.route) {
                                popUpTo(MementoRoute.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
