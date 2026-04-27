package com.jstr14.picaday.ui.daydetail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jstr14.picaday.domain.model.DayEntry
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DayImageSection(
    entry: DayEntry,
    pagerState: PagerState,
    date: String,
    sheetState: AnchoredDraggableState<Boolean>,
    collapsedOffsetPx: Float,
    sheetOffsetPx: Float,
    sheetProgress: Float,
    isZoomed: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    isUploading: Boolean,
    isDeleting: Boolean,
    onBack: () -> Unit,
    onSaveToGallery: (String) -> Unit,
    onDeletePhoto: (LocalDate, String) -> Unit,
    onDeleteAll: () -> Unit,
    onAddPhotos: (LocalDate, List<android.net.Uri>) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }
    var pendingDeleteUrl by remember { mutableStateOf<String?>(null) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    var showDeleteDayDialog by remember { mutableStateOf(false) }

    val savePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingSaveUrl?.let { onSaveToGallery(it) }
        pendingSaveUrl = null
    }

    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) onAddPhotos(LocalDate.parse(date), uris)
    }

    val mediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Whether granted or denied, open the picker — GPS will work if granted
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchPickerWithLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            mediaLocationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    if (showDeletePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePhotoDialog = false; pendingDeleteUrl = null },
            title = { Text("¿Eliminar foto?") },
            text = { Text("Esta foto se eliminará permanentemente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteUrl?.let { onDeletePhoto(LocalDate.parse(date), it) }
                        pendingDeleteUrl = null
                        showDeletePhotoDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePhotoDialog = false; pendingDeleteUrl = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteDayDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDayDialog = false },
            title = { Text("¿Borrar día completo?") },
            text = { Text("Se eliminarán permanentemente todas las fotos y el texto de este día.") },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteAll(); showDeleteDayDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
            .graphicsLayer { translationY = -(collapsedOffsetPx - sheetOffsetPx) }
            .pointerInput(isZoomed) {
                if (isZoomed) return@pointerInput
                var velocityTracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = { velocityTracker = VelocityTracker() },
                    onVerticalDrag = { change, dragAmount ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        sheetState.dispatchRawDelta(dragAmount)
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity()
                        coroutineScope.launch { sheetState.settle(velocity.y) }
                    },
                    onDragCancel = {
                        coroutineScope.launch { sheetState.settle(0f) }
                    }
                )
            }
    ) {
        if (entry.imageUrls.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isZoomed
            ) { page ->
                var scale by remember { mutableStateOf(1f) }
                var panOffset by remember { mutableStateOf(Offset.Zero) }
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != page) {
                        scale = 1f; panOffset = Offset.Zero; onZoomChanged(false)
                    }
                }
                AsyncImage(
                    model = entry.imageUrls[page],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                scale = 1f; panOffset = Offset.Zero; onZoomChanged(false)
                            })
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val multiTouch = event.changes.count { it.pressed } >= 2
                                    if (multiTouch || scale > 1f) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                        scale = newScale
                                        panOffset = if (newScale <= 1f) Offset.Zero else panOffset + panChange
                                        onZoomChanged(newScale > 1f)
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = panOffset.x; translationY = panOffset.y
                        },
                    contentScale = ContentScale.Fit
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            )
        }

        // Top bar — back | date+time (center) | fav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = 1f - sheetProgress },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.toShortDate(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                val currentPhotoTime = entry.photos.getOrNull(pagerState.currentPage)?.time
                if (currentPhotoTime != null) {
                    Text(
                        text = currentPhotoTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            IconButton(
                onClick = { Toast.makeText(context, "Favorito (próximamente)", Toast.LENGTH_SHORT).show() }
            ) {
                Icon(Icons.Outlined.FavoriteBorder, "Favorito", tint = Color.White)
            }
        }

        // Page indicator — overlaid on image, above action row
        if (entry.imageUrls.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${entry.imageUrls.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 152.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        // FAB + uploading indicator + action row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
        ) {
            AnimatedVisibility(
                visible = isUploading,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Añadiendo recuerdos...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isDeleting,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Eliminando...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = { launchPickerWithLocationPermission() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, "Añadir")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                BottomAction(Icons.Default.Share, "Share", modifier = Modifier.weight(1f)) {
                    Toast.makeText(context, "Compartir (próximamente)", Toast.LENGTH_SHORT).show()
                }
                BottomAction(Icons.Default.SaveAlt, "Save", modifier = Modifier.weight(1f)) {
                    val url = entry.imageUrls.getOrNull(pagerState.currentPage) ?: return@BottomAction
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        pendingSaveUrl = url
                        savePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        onSaveToGallery(url)
                    }
                }
                BottomAction(Icons.Default.DeleteOutline, "Remove", modifier = Modifier.weight(1f)) {
                    if (!isDeleting) entry.imageUrls.getOrNull(pagerState.currentPage)?.let { url ->
                        pendingDeleteUrl = url
                        showDeletePhotoDialog = true
                    }
                }
                BottomAction(
                    icon = Icons.Default.DeleteForever,
                    label = "Remove all",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    onClick = { if (!isDeleting) showDeleteDayDialog = true }
                )
            }
        }
    }
}
