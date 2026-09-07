package com.tangai.memento.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangai.memento.core.designsystem.theme.MementoTheme
import com.tangai.memento.feature.auth.presentation.ui.ForgotPasswordScreen
import com.tangai.memento.feature.auth.presentation.ui.LoginScreen
import com.tangai.memento.feature.auth.presentation.ui.RegisterScreen
import com.tangai.memento.feature.auth.presentation.ui.SplashScreen
import com.tangai.memento.feature.auth.presentation.viewmodel.LogoutEffect
import com.tangai.memento.feature.auth.presentation.viewmodel.LogoutViewModel
import com.tangai.memento.feature.connection.presentation.ui.ConnectionScreen
import com.tangai.memento.feature.history.presentation.ui.HistoryScreen
import com.tangai.memento.feature.home.presentation.ui.HomeScreen
import com.tangai.memento.feature.home.presentation.ui.ProfileScreen
import com.tangai.memento.feature.post.presentation.ui.CreatePostScreen
import com.tangai.memento.feature.post.presentation.ui.MediaPickerScreen
import com.tangai.memento.feature.post.presentation.ui.MediaPreviewScreen

@Composable
fun MementoNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        MementoRoute.Home.route,
        MementoRoute.Connection.route,
        MementoRoute.History.route,
        MementoRoute.Profile.route
    )
    val navItems = listOf(
        MementoRoute.Home to Icons.Filled.Home,
        MementoRoute.Connection to Icons.Filled.People,
        MementoRoute.History to Icons.Filled.History,
        MementoRoute.Profile to Icons.Filled.Person
    )

    MementoTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showBottomBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp), clip = false)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            navController.navigate(MementoRoute.Home.route) {
                                popUpTo(MementoRoute.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Home",
                                tint = if (currentRoute == MementoRoute.Home.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(MementoRoute.Connection.route) {
                                popUpTo(MementoRoute.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = "Connections",
                                tint = if (currentRoute == MementoRoute.Connection.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(MementoRoute.CreatePost.route)
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Create Post",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(MementoRoute.History.route) {
                                popUpTo(MementoRoute.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "History",
                                tint = if (currentRoute == MementoRoute.History.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(MementoRoute.Profile.route) {
                                popUpTo(MementoRoute.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = if (currentRoute == MementoRoute.Profile.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
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
                        },
                        onNavigateToHome = {
                            navController.navigate(MementoRoute.Home.route) {
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
                    val logoutViewModel: LogoutViewModel = hiltViewModel()
                    LaunchedEffect(Unit) {
                        logoutViewModel.effect.collect { effect ->
                            when (effect) {
                                LogoutEffect.NavigateToLogin -> {
                                    navController.navigate(MementoRoute.Login.route) {
                                        popUpTo(MementoRoute.Home.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }
                    HomeScreen(
                    )
                }

                composable(MementoRoute.Connection.route) {
                    ConnectionScreen()
                }

                composable(MementoRoute.History.route) {
                    HistoryScreen()
                }

                composable(MementoRoute.Profile.route) {
                    val logoutViewModel: LogoutViewModel = hiltViewModel()
                    LaunchedEffect(Unit) {
                        logoutViewModel.effect.collect { effect ->
                            when (effect) {
                                LogoutEffect.NavigateToLogin -> {
                                    navController.navigate(MementoRoute.Login.route) {
                                        popUpTo(MementoRoute.Home.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }
                    ProfileScreen(
                        onLogout = { logoutViewModel.logout() }
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
