package com.example.sinpe_validation_app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MainScreen() {
    Scaffold { paddingValues ->
        // Usamos un Box para aplicar el padding y que InboxScreen no tire error
        Box(modifier = Modifier.padding(paddingValues)) {
            InboxScreen()
        }
    }
}