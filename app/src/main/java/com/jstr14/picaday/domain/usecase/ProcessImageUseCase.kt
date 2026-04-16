package com.jstr14.picaday.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.jstr14.picaday.core.utils.ImageHelper
import com.jstr14.picaday.domain.model.ProcessedImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ProcessImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageHelper: ImageHelper
) {
    suspend operator fun invoke(uri: Uri): ProcessedImage? = withContext(Dispatchers.IO) {
        try {
            // 1. Comprimir (usando el helper)
            val bytes = imageHelper.compressImage(uri) ?: return@withContext null

            // 2. Extraer fecha (EXIF)
            val date = extractDateFromExif(uri) ?: LocalDate.now()

            ProcessedImage(bytes, date)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDateFromExif(uri: Uri): LocalDate? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                if (dateString != null) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                    LocalDate.parse(dateString, formatter)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}