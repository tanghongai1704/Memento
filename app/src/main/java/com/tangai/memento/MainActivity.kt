package com.tangai.memento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import dagger.hilt.android.AndroidEntryPoint
import com.tangai.memento.navigation.MementoNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val systemDarkTheme = isSystemInDarkTheme()
            val preferences = remember {
                context.getSharedPreferences("memento_preferences", MODE_PRIVATE)
            }
            var darkTheme by remember {
                mutableStateOf(
                    preferences.getBoolean("dark_theme", systemDarkTheme)
                )
            }

            MementoNavGraph(
                darkTheme = darkTheme,
                onDarkThemeChanged = { enabled ->
                    darkTheme = enabled
                    preferences.edit { putBoolean("dark_theme", enabled) }
                }
            )
        }
    }
}
