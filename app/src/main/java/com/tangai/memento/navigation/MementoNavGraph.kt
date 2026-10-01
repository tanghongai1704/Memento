package com.tangai.memento.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tangai.memento.core.designsystem.theme.MementoTheme
import com.tangai.memento.feature.auth.presentation.ui.ForgotPasswordScreen
import com.tangai.memento.feature.auth.presentation.ui.LoginScreen
import com.tangai.memento.feature.auth.presentation.ui.RegisterScreen
import com.tangai.memento.feature.auth.presentation.ui.SplashScreen
import com.tangai.memento.feature.auth.presentation.viewmodel.LogoutEffect
import com.tangai.memento.feature.auth.presentation.viewmodel.LogoutViewModel
import com.tangai.memento.feature.connection.presentation.ui.ConnectionScreen
import com.tangai.memento.feature.home.presentation.ui.HomeScreen
import com.tangai.memento.feature.home.presentation.ui.ProfileScreen
import com.tangai.memento.feature.post.presentation.ui.MediaPickerScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MementoNavGraph(
    darkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        MementoRoute.Home.route,
        MementoRoute.Connection.route,
        MementoRoute.Profile.route
    )
    val navItems = listOf(
        Triple(MementoRoute.Home, Icons.Filled.Home, "Home"),
        Triple(MementoRoute.Connection, Icons.Filled.People, "Connections"),
        Triple(MementoRoute.Profile, Icons.Filled.Person, "Profile")
    )

    DisposableEffect(navController) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val route = navController.currentDestination?.route
            val protectedRoutes = setOf(
                MementoRoute.Home.route,
                MementoRoute.Connection.route,
                MementoRoute.Profile.route,
                MementoRoute.CreatePost.route
            )
            if (firebaseAuth.currentUser == null && route in protectedRoutes) {
                navController.navigate(MementoRoute.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    MementoTheme(darkTheme = darkTheme) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (showBottomBar) {
                    FloatingActionButton(
                        onClick = { navController.navigate(MementoRoute.CreatePost.route) },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create moment")
                    }
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        navItems.forEach { (route, icon, label) ->
                            val selected = backStackEntry?.destination?.hierarchy
                                ?.any { it.route == route.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(route.route) {
                                        popUpTo(MementoRoute.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                        onCreateMoment = { navController.navigate(MementoRoute.CreatePost.route) },
                        onOpenConnections = { navController.navigate(MementoRoute.Connection.route) }
                    )
                }

                composable(MementoRoute.Connection.route) {
                    ConnectionScreen()
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
                        isDarkTheme = darkTheme,
                        onDarkThemeChanged = onDarkThemeChanged,
                        onLogout = { logoutViewModel.logout() }
                    )
                }

                composable(MementoRoute.CreatePost.route) {
                    MediaPickerScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToHome = {
                            navController.navigate(MementoRoute.Home.route) {
                                popUpTo(MementoRoute.Home.route) { inclusive = true }
                            }
                        },
                        onNavigateToConnections = {
                            navController.navigate(MementoRoute.Connection.route) {
                                popUpTo(MementoRoute.Home.route) { saveState = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
