package com.jstr14.picaday.ui.daydetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    var showDeleteDayDialog by remember { mutableStateOf(false) }

    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val localDate = LocalDate.parse(date)
            viewModel.addMultiplePhotosToSpecificDay(
                date = localDate,
                uris = uris
            )
        }
    }

    LaunchedEffect(date) {
        viewModel.loadData(date)
    }

    // Confirmation dialog for deleting the whole day
    if (showDeleteDayDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDayDialog = false },
            title = { Text("¿Borrar día completo?") },
            text = { Text("Se eliminarán permanentemente todas las fotos y el texto de este día.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFullDay(LocalDate.parse(date)) {
                            onBack()
                        }
                        showDeleteDayDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Borrar Todo") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDayDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            // State 1: Loading data
            isLoading && dayEntry == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text("Buscando recuerdos...", color = Color.White)
                }
            }

            // State 2: Empty data
            !isLoading && dayEntry == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No hay recuerdos el ${date.toPrettyDate()}",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Pulsa el botón + para añadir tu primera foto.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // State 3: Show Data
            else -> {
                dayEntry?.let { entry ->
                    val pagerState = rememberPagerState { entry.imageUrls.size }

                    if (entry.imageUrls.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = entry.imageUrls[page],
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // Top gradient scrim for icon visibility
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                                    )
                                )
                        )

                        // Page indicator
                        Text(
                            text = "${pagerState.currentPage + 1} / ${entry.imageUrls.size}",
                            color = Color.White,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 60.dp, end = 16.dp)
                                .align(Alignment.TopEnd)
                                .background(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge
                        )

                        // Remove current photo
                        IconButton(
                            onClick = {
                                val currentUrl = entry.imageUrls[pagerState.currentPage]
                                viewModel.deletePhoto(LocalDate.parse(date), currentUrl)
                            },
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 8.dp, end = 56.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.DeleteOutline, "Borrar foto", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    // Remove whole day data
                    IconButton(
                        onClick = { showDeleteDayDialog = true },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.DeleteForever, "Borrar todo", tint = MaterialTheme.colorScheme.error)
                    }

                    // Bottom panel with text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f), Color.Black)
                                )
                            )
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 85.dp, top = 60.dp, bottom = 32.dp)
                    ) {
                        Column {
                            Text(date.toPrettyDate(), color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = entry.description ?: "Sin descripción.",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Uploading indicator
        AnimatedVisibility(
            visible = isUploading,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Añadiendo recuerdos...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }

        // back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White)
        }

        // FAB
        FloatingActionButton(
            onClick = {
                pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.AddPhotoAlternate, "Añadir")
        }
    }
}

fun String.toPrettyDate(): String {
    return try {
        val localDate = LocalDate.parse(this)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        localDate.format(formatter)
    } catch (e: Exception) {
        this
    }
}