package com.jstr14.picaday.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.jstr14.picaday.core.utils.ImageHelper
import com.jstr14.picaday.domain.model.ProcessedImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ProcessImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageHelper: ImageHelper
) {
    suspend operator fun invoke(uri: Uri): ProcessedImage? = withContext(Dispatchers.IO) {
        try {
            val bytes = imageHelper.compressImage(uri) ?: return@withContext null
            val exifData = extractExifData(uri)
            ProcessedImage(
                bytes = bytes,
                date = exifData.date ?: LocalDate.now(),
                time = exifData.time,
                lat = exifData.lat,
                lon = exifData.lon,
            )
        } catch (e: Exception) {
            null
        }
    }

    private data class ExifData(
        val date: LocalDate?,
        val time: String?,
        val lat: Double?,
        val lon: Double?,
    )

    private fun extractExifData(uri: Uri): ExifData {
        return try {
            val exif = openExifInterface(uri) ?: return ExifData(null, null, null, null)

            val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val (date, time) = if (dateString != null) {
                val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                val dateTime = LocalDateTime.parse(dateString, formatter)
                Pair(dateTime.toLocalDate(), dateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
            } else Pair(null, null)

            val (lat, lon) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                exif.latLong?.let { Pair(it[0], it[1]) } ?: Pair(null, null)
            } else {
                val latLong = FloatArray(2)
                @Suppress("DEPRECATION")
                if (exif.getLatLong(latLong)) Pair(latLong[0].toDouble(), latLong[1].toDouble())
                else Pair(null, null)
            }

            ExifData(date, time, lat, lon)
        } catch (e: Exception) {
            ExifData(null, null, null, null)
        }
    }

    /**
     * Opens an ExifInterface with unredacted GPS access.
     *
     * Strategy 1 – setRequireOriginal + openInputStream (official Android pattern for direct
     *   MediaStore URIs on Android 10+).
     * Strategy 2 – openFileDescriptor. On Android 14+ with ACCESS_MEDIA_LOCATION granted, Photo
     *   Picker URIs expose the raw file via a file descriptor, giving full unredacted EXIF.
     * Strategy 3 – Direct openInputStream fallback.
     */
    private fun openExifInterface(uri: Uri): ExifInterface? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val original = MediaStore.setRequireOriginal(uri)
                context.contentResolver.openInputStream(original)?.use { stream ->
                    return ExifInterface(stream)
                }
            }
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    return ExifInterface(pfd.fileDescriptor)
                }
            }
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        }.getOrNull()
    }
}