package com.jstr14.picaday.ui.daydetail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jstr14.picaday.ui.theme.logo.PicADayLogo
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayDetailScreen(
    date: String,
    onBack: () -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel()
) {
    val dayEntry by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val saveResult by viewModel.saveToGalleryResult.collectAsState()

    val context = LocalContext.current
    val density = LocalDensity.current
    var isEditingDescription by remember { mutableStateOf(false) }
    var descriptionText by remember(dayEntry?.description) { mutableStateOf(dayEntry?.description ?: "") }
    val focusRequester = remember { FocusRequester() }
    var isZoomed by remember { mutableStateOf(false) }
    val descriptionBringIntoViewRequester = remember { BringIntoViewRequester() }

    val imageCount = dayEntry?.imageUrls?.size ?: 0
    val pagerState = rememberPagerState { imageCount }
    val hasData = !isLoading && dayEntry != null
    var sheetContentHeightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(saveResult) {
        saveResult ?: return@LaunchedEffect
        val message = if (saveResult is SaveResult.Success) "Guardado en galería" else "Error al guardar"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearSaveResult()
    }

    LaunchedEffect(isEditingDescription) {
        if (isEditingDescription) focusRequester.requestFocus()
    }

    LaunchedEffect(descriptionText) {
        if (isEditingDescription) descriptionBringIntoViewRequester.bringIntoView()
    }

    LaunchedEffect(date) { viewModel.loadData(date) }

    val pickMediaForEmptyState = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addMultiplePhotosToSpecificDay(LocalDate.parse(date), uris)
    }

    val mediaLocationPermissionForEmptyState = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pickMediaForEmptyState.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchEmptyStatePickerWithLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            mediaLocationPermissionForEmptyState.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            pickMediaForEmptyState.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    val sheetState = remember {
        AnchoredDraggableState(
            initialValue = false,
            positionalThreshold = { it * 0.05f },
            velocityThreshold = { with(density) { 20.dp.toPx() } },
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(isEditingDescription) {
        if (isEditingDescription) sheetState.animateTo(true)
    }

    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val peekHeightPx = with(density) { 20.dp.toPx() }
        val collapsedOffsetPx = screenHeightPx - peekHeightPx
        val statusBarTopPx = with(density) {
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
        }
        val minSheetTopOffsetPx = statusBarTopPx + with(density) { 16.dp.toPx() }
        val maxSheetTopOffsetPx = screenHeightPx * 0.45f
        val expandedOffsetPx = if (sheetContentHeightPx > 0f) {
            (screenHeightPx - sheetContentHeightPx).coerceIn(minSheetTopOffsetPx, maxSheetTopOffsetPx)
        } else {
            maxSheetTopOffsetPx
        }

        SideEffect {
            if (hasData) {
                sheetState.updateAnchors(DraggableAnchors {
                    false at collapsedOffsetPx
                    true at expandedOffsetPx
                })
            }
        }

        val rawOffset = sheetState.offset
        val sheetOffsetPx = if (rawOffset.isNaN()) collapsedOffsetPx else rawOffset
        val sheetProgress = ((collapsedOffsetPx - sheetOffsetPx) / (collapsedOffsetPx - expandedOffsetPx))
            .coerceIn(0f, 1f)

        if (isLoading && dayEntry == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text("Buscando recuerdos...", color = Color.White)
            }
        }

        if (!isLoading && dayEntry == null) {
            EmptyDayContent(date = date, isUploading = isUploading)
        }

        dayEntry?.let { entry ->
            DayImageSection(
                entry = entry,
                pagerState = pagerState,
                date = date,
                sheetState = sheetState,
                collapsedOffsetPx = collapsedOffsetPx,
                sheetOffsetPx = sheetOffsetPx,
                sheetProgress = sheetProgress,
                isZoomed = isZoomed,
                onZoomChanged = { isZoomed = it },
                isUploading = isUploading,
                isDeleting = isDeleting,
                onSaveToGallery = { viewModel.saveImageToGallery(it) },
                onDeletePhoto = { d, url -> viewModel.deletePhoto(d, url) },
                onBack = onBack,
                onDeleteAll = { viewModel.deleteFullDay(LocalDate.parse(date)) { onBack() } },
                onAddPhotos = { d, uris -> viewModel.addMultiplePhotosToSpecificDay(d, uris) }
            )
            DayDetailSheet(
                entry = entry,
                date = date,
                currentPhotoIndex = pagerState.currentPage,
                sheetState = sheetState,
                sheetOffsetPx = sheetOffsetPx,
                maxSheetHeightDp = with(density) { (screenHeightPx - minSheetTopOffsetPx).toDp() },
                onSizeChanged = { sheetContentHeightPx = it.toFloat() },
                isEditingDescription = isEditingDescription,
                onEditingDescriptionChanged = { isEditingDescription = it },
                descriptionText = descriptionText,
                onDescriptionTextChanged = { descriptionText = it },
                focusRequester = focusRequester,
                bringIntoViewRequester = descriptionBringIntoViewRequester,
                onSaveDescription = { d, text -> viewModel.updateDescription(d, text) }
            )
        }

        // Back button — only for loading / empty states (data state has its own in DayImageSection)
        if (dayEntry == null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(4.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        // FAB for empty state — hidden while uploading
        if (!isLoading && !isUploading && dayEntry == null) {
            FloatingActionButton(
                onClick = { launchEmptyStatePickerWithLocationPermission() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.AddPhotoAlternate, "Añadir fotos")
            }
        }
    }
}

@Composable
private fun EmptyDayContent(date: String, isUploading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Guardando tus recuerdos...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PicADayLogo(logoSize = 130.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = date.toPrettyDate(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Aún no hay recuerdos para este día",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pulsa el botón de abajo para añadir tu primer recuerdo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
