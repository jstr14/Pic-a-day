package com.jstr14.picaday.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.ui.auth.AuthViewModel
import com.jstr14.picaday.ui.auth.AuthState

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onSignInClick: () -> Unit
) {
    // Observamos el estado de la UI del ViewModel
    val uiState by authViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Pic-a-Day",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (uiState) {
            is AuthState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthState.Error -> {
                Text(
                    text = (uiState as AuthState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LoginButton(onClick = onSignInClick)
            }
            else -> {
                LoginButton(onClick = onSignInClick)
            }
        }
    }
}

@Composable
fun LoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = "Sign in with Google")
    }
}