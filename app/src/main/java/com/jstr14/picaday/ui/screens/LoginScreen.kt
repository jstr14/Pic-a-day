package com.jstr14.picaday.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.ui.auth.AuthState
import com.jstr14.picaday.ui.auth.AuthViewModel
import com.jstr14.picaday.ui.theme.logo.PicADayLogo

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onSignInClick: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()

    if (uiState is AuthState.Error) {
        AlertDialog(
            onDismissRequest = { authViewModel.clearError() },
            title = { Text("Sign-In failed") },
            text = { Text((uiState as AuthState.Error).message) },
            confirmButton = {
                TextButton(onClick = { authViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SeasonalTopWave(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo sits inside the primary-colored top area
            Spacer(Modifier.weight(0.5f))
            PicADayLogo(logoSize = 150.dp)
            Spacer(Modifier.weight(0.9f))

            // Text sits in the background-colored bottom area, below the wave
            Text(
                text = "PicADay",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Capture a moment every day",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
                if (uiState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    SignInButton(onClick = onSignInClick)
                }
            }

            Spacer(Modifier.weight(0.4f))
        }
    }
}

@Composable
private fun SeasonalTopWave(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Single cubic bezier for the wave bottom edge — no junctions, no kinks.
        // CP1 stays near baseY to keep the right side flat; CP2 creates the broad lobe.
        val baseY = h * 0.40f
        val cp1X  = w * 0.78f;  val cp1Y = baseY
        val cp2X  = w * 0.26f;  val cp2Y = h * 0.56f

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, baseY)
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, 0f, baseY)
            close()
        }
        drawPath(path, color = primary)

        // Topographic contour lines in the background area, echoing the wave below it
        for (i in 1..5) {
            val dy = h * 0.038f * i
            val contour = Path().apply {
                moveTo(w, baseY + dy)
                cubicTo(cp1X, cp1Y + dy, cp2X, cp2Y + dy, 0f, baseY + dy)
            }
            drawPath(contour, color = primary.copy(alpha = 0.13f), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
private fun SignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = "Sign in with Google",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}