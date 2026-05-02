package com.jstr14.picaday.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import android.content.Intent
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import com.jstr14.picaday.MainActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jstr14.picaday.R
import com.jstr14.picaday.data.model.DayEntryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max

class MemoryWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    // Three supported sizes; Glance picks the largest that fits the actual widget dimensions.
    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL,   // 2×2 — photo only
            MEDIUM,  // 4×2 — photo + date bar
            LARGE,   // 4×4 — photo + larger date bar
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)

        // First render or stale: fetch data inline.
        // provideGlance is a suspend function with a 45s timeout — safe for network I/O.
        if ((prefs[WidgetKeys.STATUS] ?: WidgetStatus.LOADING) == WidgetStatus.LOADING) {
            fetchAndUpdateState(context, id)
            prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        }

        val status = prefs[WidgetKeys.STATUS] ?: WidgetStatus.LOADING
        val imagePaths = (prefs[WidgetKeys.IMAGE_PATHS] ?: emptySet()).sorted()
        val currentIndex = prefs[WidgetKeys.CURRENT_INDEX] ?: 0
        val dateLabel = prefs[WidgetKeys.DATE_LABEL] ?: ""
        val imageCount = prefs[WidgetKeys.IMAGE_COUNT] ?: 0
        val foundDate = prefs[WidgetKeys.FOUND_DATE]

        val bitmap: Bitmap? = if (status == WidgetStatus.READY && imagePaths.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val path = imagePaths.getOrElse(currentIndex) { imagePaths.first() }
                if (File(path).exists()) loadScaledBitmap(path) else null
            }
        } else null

        provideContent {
            WidgetContent(
                status = status,
                bitmap = bitmap,
                dateLabel = dateLabel,
                currentIndex = currentIndex,
                imageCount = imageCount,
                foundDate = foundDate,
            )
        }
    }

    companion object {
        val SMALL = DpSize(140.dp, 140.dp)
        val MEDIUM = DpSize(250.dp, 140.dp)
        val LARGE = DpSize(250.dp, 250.dp)

        suspend fun fetchAndUpdateState(context: Context, id: GlanceId) {
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    saveState(context, id, WidgetStatus.NOT_LOGGED_IN, emptyList(), "")
                    return
                }

                val uid = auth.currentUser!!.uid
                val firestore = FirebaseFirestore.getInstance()
                val today = LocalDate.now()

                var foundDate: LocalDate? = null
                var foundUrls: List<String> = emptyList()

                for (yearsBack in 1..5) {
                    val date = today.minusYears(yearsBack.toLong())
                    val snapshot = firestore
                        .collection("users").document(uid)
                        .collection("entries").document(date.toString())
                        .get().await()

                    val dto = snapshot.toObject(DayEntryDto::class.java) ?: continue
                    val urls = imageUrlsFromDto(dto)
                    if (urls.isNotEmpty()) {
                        foundDate = date
                        foundUrls = urls
                        break
                    }
                }

                if (foundDate == null) {
                    saveState(context, id, WidgetStatus.NO_MEMORIES, emptyList(), "")
                    return
                }

                val yearsBack = today.year - foundDate.year
                val cacheDir = File(context.filesDir, "widget_cache").also { it.mkdirs() }
                cacheDir.listFiles()?.forEach { it.delete() }

                val cachedPaths = mutableListOf<String>()
                foundUrls.forEachIndexed { index, url ->
                    val file = File(cacheDir, "photo_$index.jpg")
                    if (downloadImage(url, file)) cachedPaths.add(file.absolutePath)
                }

                if (cachedPaths.isEmpty()) {
                    saveState(context, id, WidgetStatus.NO_MEMORIES, emptyList(), "")
                    return
                }

                saveState(
                    context,
                    id,
                    WidgetStatus.READY,
                    cachedPaths,
                    buildDateLabel(context, yearsBack, foundDate),
                    foundDate.toString()
                )
            } catch (_: Exception) {
                // Leave status as LOADING so the next provideGlance call retries
            }
        }

        private fun imageUrlsFromDto(dto: DayEntryDto): List<String> {
            val fromPhotos = dto.photos.mapNotNull { it.url.ifBlank { null } }
            return fromPhotos.ifEmpty { dto.imageUrls.filter { it.isNotBlank() } }
        }

        private suspend fun saveState(
            context: Context,
            id: GlanceId,
            status: String,
            imagePaths: List<String>,
            dateLabel: String,
            foundDate: String = "",
        ) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WidgetKeys.STATUS] = status
                    this[WidgetKeys.IMAGE_PATHS] = imagePaths.toSet()
                    this[WidgetKeys.DATE_LABEL] = dateLabel
                    this[WidgetKeys.CURRENT_INDEX] = 0
                    this[WidgetKeys.IMAGE_COUNT] = imagePaths.size
                    this[WidgetKeys.FOUND_DATE] = foundDate
                }
            }
        }

        private suspend fun downloadImage(url: String, file: File): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    URL(url).openStream().use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    BitmapFactory.decodeFile(file.absolutePath) != null
                } catch (_: Exception) {
                    file.delete()
                    false
                }
            }

        private fun buildDateLabel(context: Context, yearsBack: Int, date: LocalDate): String {
            val yearPart = context.resources.getQuantityString(R.plurals.widget_years_ago, yearsBack, yearsBack)
            val dateStr = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .format(date)
            return "$yearPart · $dateStr"
        }

        fun loadScaledBitmap(path: String): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val longSide = max(bounds.outWidth, bounds.outHeight).takeIf { it > 0 } ?: return null
            val opts = BitmapFactory.Options().apply { inSampleSize = max(1, longSide / 500) }
            return BitmapFactory.decodeFile(path, opts)
        }
    }
}

@Composable
private fun WidgetContent(
    status: String,
    bitmap: Bitmap?,
    dateLabel: String,
    currentIndex: Int,
    imageCount: Int,
    foundDate: String?,
) {
    val size = LocalSize.current
    val isSmall = size.width <= MemoryWidget.SMALL.width
    val context = LocalContext.current

    val openAppModifier = if (!foundDate.isNullOrBlank()) {
        GlanceModifier.clickable(
            actionStartActivity(
                Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_WIDGET_DATE, foundDate)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
        )
    } else GlanceModifier

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFF1C1B1F)))
            .then(openAppModifier),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            WidgetStatus.READY -> {
                if (isSmall) {
                    SmallReadyContent(bitmap)
                } else {
                    ReadyContent(bitmap, dateLabel, currentIndex, imageCount)
                }
            }

            WidgetStatus.NOT_LOGGED_IN -> CenteredText(context.getString(R.string.widget_sign_in))
            WidgetStatus.NO_MEMORIES -> CenteredText(context.getString(R.string.widget_no_memories))
            else -> CenteredText(context.getString(R.string.widget_loading))
        }
    }
}

@Composable
private fun SmallReadyContent(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ReadyContent(
    bitmap: Bitmap?,
    dateLabel: String,
    currentIndex: Int,
    imageCount: Int,
) {
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize(),
        )
    }

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(day = Color(0xAA000000), night = Color(0xAA000000)))
                .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
        ) {
            Text(
                text = dateLabel,
                style = TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White),
                    fontSize = 10.sp,
                ),
            )
            if (imageCount > 1) {
                Text(
                    text = "${currentIndex + 1} / $imageCount",
                    style = TextStyle(
                        color = ColorProvider(day = Color(0xCCFFFFFF), night = Color(0xCCFFFFFF)),
                        fontSize = 9.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CenteredText(text: String) {
    Text(
        text = text,
        modifier = GlanceModifier.padding(16.dp),
        style = TextStyle(
            color = ColorProvider(day = Color(0xAAFFFFFF), night = Color(0xAAFFFFFF)),
            fontSize = 13.sp,
        ),
    )
}
