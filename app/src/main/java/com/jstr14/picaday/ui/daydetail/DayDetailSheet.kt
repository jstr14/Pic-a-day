package com.jstr14.picaday.ui.daydetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jstr14.picaday.domain.model.DayEntry
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun DayDetailSheet(
    entry: DayEntry,
    date: String,
    currentPhotoIndex: Int,
    sheetState: AnchoredDraggableState<Boolean>,
    sheetOffsetPx: Float,
    maxSheetHeightDp: Dp,
    onSizeChanged: (Int) -> Unit,
    isEditingDescription: Boolean,
    onEditingDescriptionChanged: (Boolean) -> Unit,
    descriptionText: String,
    onDescriptionTextChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    onSaveDescription: (LocalDate, String) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sheetScrollState = rememberScrollState()
    val nestedScrollConnection = remember(sheetState, sheetScrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return when {
                    delta > 0 && sheetScrollState.value == 0 -> {
                        Offset(0f, sheetState.dispatchRawDelta(delta))
                    }
                    delta < 0 && sheetState.currentValue == false -> {
                        Offset(0f, sheetState.dispatchRawDelta(delta))
                    }
                    else -> Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return when {
                    available.y > 0 && sheetScrollState.value == 0 -> {
                        sheetState.settle(available.y); available
                    }
                    available.y < 0 && sheetState.currentValue == false -> {
                        sheetState.settle(available.y); available
                    }
                    else -> Velocity.Zero
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, sheetOffsetPx.roundToInt()) }
            .heightIn(max = maxSheetHeightDp)
            .onSizeChanged { onSizeChanged(it.height) }
            .background(Color(0xFF1C1C1E), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .anchoredDraggable(sheetState, Orientation.Vertical)
                .padding(top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(sheetScrollState)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = date.toLongDate(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            val currentPhoto = entry.photos.getOrNull(currentPhotoIndex)
            val albumNames = currentPhoto?.albumNames.orEmpty()

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(12.dp))

            // Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (isEditingDescription) {
                    Box(modifier = Modifier.weight(1f).bringIntoViewRequester(bringIntoViewRequester)) {
                        if (descriptionText.isEmpty()) {
                            Text(
                                "Añadir tu descripción...",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        BasicTextField(
                            value = descriptionText,
                            onValueChange = onDescriptionTextChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, lineHeight = 24.sp),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                onEditingDescriptionChanged(false)
                                onSaveDescription(LocalDate.parse(date), descriptionText)
                                focusManager.clearFocus()
                            })
                        )
                    }
                    IconButton(onClick = {
                        onEditingDescriptionChanged(false)
                        onSaveDescription(LocalDate.parse(date), descriptionText)
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Check, "Confirmar", tint = Color.White)
                    }
                } else {
                    Text(
                        text = entry.description.takeIf { !it.isNullOrBlank() } ?: "Añadir tu descripción...",
                        color = if (entry.description.isNullOrBlank()) Color.White.copy(alpha = 0.4f) else Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        onDescriptionTextChanged(entry.description ?: "")
                        onEditingDescriptionChanged(true)
                    }) {
                        Icon(Icons.Default.Edit, "Editar descripción", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            if (albumNames.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Álbumes",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    albumNames.forEach { name ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF2C2C2E),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoAlbum,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(16.dp))

            // Location card — shows location of the currently visible photo
            val lat = currentPhoto?.lat
            val lon = currentPhoto?.lon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2C2E))
                    .then(
                        if (lat != null && lon != null) Modifier.clickable {
                            val uri = Uri.parse("geo:$lat,$lon?z=15&q=$lat,$lon")
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: Exception) {
                                val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lon")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            }
                        } else Modifier
                    )
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = if (lat != null) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (lat != null && lon != null) {
                        Text(
                            "%.5f, %.5f".format(lat, lon),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Sin ubicación", color = Color.White.copy(alpha = 0.25f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (lat != null && lon != null) {
                    Text(
                        text = "Abrir en Google Maps",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
