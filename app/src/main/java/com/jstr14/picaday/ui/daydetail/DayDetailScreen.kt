package com.jstr14.picaday.ui.daydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    onBack: () -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel()
) {
    val dayEntry by viewModel.state.collectAsState()

    LaunchedEffect(date) {
        viewModel.loadData(date)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fondo negro estilo galería
    ) {
        dayEntry?.let { entry ->
            val pagerState = rememberPagerState {
                entry.imageUrls.size
            }

            // 1. Back Layer (Pager) - Main content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp
            ) { page ->
                AsyncImage(
                    model = entry.imageUrls[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // 2. Top Layer: Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // 3. Top layer: Text with gradient and description
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter) // Ahora sí funciona correctamente
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black
                            )
                        )
                    )
                    .navigationBarsPadding() // Respeta la barra de gestos de Android
                    .padding(start = 20.dp, end = 85.dp, top = 60.dp, bottom = 32.dp)
            ) {
                Column {
                    Text(
                        text = date.toPrettyDate(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = entry.description ?: "Sin descripción para este día.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Top Layer: Floating Action Button
            FloatingActionButton(
                onClick = {
                    // TODO: Add functionality to add a new image
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }

            // 5. Top Layer: Page indicator
            Text(
                text = "${pagerState.currentPage + 1} / ${entry.imageUrls.size}",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
                style = MaterialTheme.typography.labelLarge
            )

        } ?: run {
            // Loading state
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}

fun String.toPrettyDate(): String {
    return try {
        val date = LocalDate.parse(this)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        date.format(formatter)
    } catch (e: Exception) {
        this
    }
}