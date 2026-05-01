package com.jstr14.picaday.ui.daydetail

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.R
import coil.compose.AsyncImage
import com.jstr14.picaday.ui.components.StatusPillIndicator
import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.model.Photo
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
    onAddPhotosClick: () -> Unit,
    albums: List<Album> = emptyList(),
    onAddToAlbum: (Photo) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }
    var pendingDeleteUrl by remember { mutableStateOf<String?>(null) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    var showDeleteDayDialog by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }

    val savePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingSaveUrl?.let { onSaveToGallery(it) }
        pendingSaveUrl = null
    }

    if (showDeletePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePhotoDialog = false; pendingDeleteUrl = null },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_photo_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteUrl?.let { onDeletePhoto(LocalDate.parse(date), it) }
                        pendingDeleteUrl = null
                        showDeletePhotoDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePhotoDialog = false; pendingDeleteUrl = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDayDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDayDialog = false },
            title = { Text(stringResource(R.string.delete_day_title)) },
            text = { Text(stringResource(R.string.delete_day_body)) },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteAll(); showDeleteDayDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDayDialog = false }) { Text(stringResource(R.string.cancel)) }
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
            if (isGridView) {
                PhotoGrid(
                    photos = entry.photos,
                    onPhotoClick = { index ->
                        isGridView = false
                        coroutineScope.launch { pagerState.scrollToPage(index) }
                    }
                )
            } else {
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
        }

        // Top bar — back (start) | date+time (true center) | grid-toggle+fav (end)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 4.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = 1f - sheetProgress },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.toShortDate(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!isGridView) {
                    val currentPhoto = entry.photos.getOrNull(pagerState.currentPage)
                    if (currentPhoto?.time != null) {
                        Text(
                            text = currentPhoto.time,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = if (isGridView) stringResource(R.string.cd_toggle_carousel) else stringResource(R.string.cd_toggle_grid),
                        tint = if (isGridView) Color.White else Color.White.copy(alpha = 0.6f)
                    )
                }
                if (!isGridView) {
                    IconButton(
                        onClick = { Toast.makeText(context, context.getString(R.string.favorite_coming_soon), Toast.LENGTH_SHORT).show() }
                    ) {
                        Icon(Icons.Outlined.FavoriteBorder, stringResource(R.string.cd_favorite), tint = Color.White)
                    }
                }
            }
        }

        // Page indicator — only in pager mode, when there are multiple images
        if (entry.imageUrls.size > 1 && !isGridView) {
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
            StatusPillIndicator(
                visible = isUploading,
                text = stringResource(R.string.uploading_memories),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            StatusPillIndicator(
                visible = isDeleting,
                text = stringResource(R.string.deleting),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = { onAddPhotosClick() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, stringResource(R.string.cd_add))
                }
            }

            // Photo-specific actions — hidden in grid mode (no current photo selected)
            if (!isGridView) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    BottomAction(Icons.Default.Share, stringResource(R.string.cd_share), modifier = Modifier.weight(1f)) {
                        Toast.makeText(context, context.getString(R.string.share_coming_soon), Toast.LENGTH_SHORT).show()
                    }
                    BottomAction(Icons.Default.SaveAlt, stringResource(R.string.cd_save), modifier = Modifier.weight(1f)) {
                        val url = entry.imageUrls.getOrNull(pagerState.currentPage) ?: return@BottomAction
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            pendingSaveUrl = url
                            savePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            onSaveToGallery(url)
                        }
                    }
                    if (albums.isNotEmpty()) {
                        BottomAction(Icons.Default.PhotoAlbum, stringResource(R.string.cd_album), modifier = Modifier.weight(1f)) {
                            entry.photos.getOrNull(pagerState.currentPage)?.let { onAddToAlbum(it) }
                        }
                    }
                    BottomAction(Icons.Default.DeleteOutline, stringResource(R.string.cd_remove), modifier = Modifier.weight(1f)) {
                        if (!isDeleting) entry.imageUrls.getOrNull(pagerState.currentPage)?.let { url ->
                            pendingDeleteUrl = url
                            showDeletePhotoDialog = true
                        }
                    }
                    BottomAction(
                        icon = Icons.Default.DeleteForever,
                        label = stringResource(R.string.label_remove_all),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isDeleting) showDeleteDayDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    onPhotoClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 56.dp),   // clear the top bar
        contentPadding = PaddingValues(bottom = 100.dp), // clear the FAB
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(photos) { index, photo ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(index) }
            ) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (photo.albumNames.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = stringResource(R.string.cd_in_album),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

