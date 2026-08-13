package com.tangai.memento.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToConnection: () -> Unit = {},
    onNavigateToMedia: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Home",
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = onNavigateToConnection,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Connection")
            }

            Button(
                onClick = onNavigateToMedia,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Media")
            }

            Button(
                onClick = onNavigateToHistory,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("History")
            }
        }
    }
}