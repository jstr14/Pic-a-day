package com.jstr14.picaday.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun compressImage(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // --- PASO NUEVO: Corregir Rotación ---
            val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, uri)

            // 1. Calcular dimensiones (Max 1080px)
            val maxSize = 1080
            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val scaleFactor = if (width > height) maxSize.toFloat() / width else maxSize.toFloat() / height

            val targetWidth = (width * scaleFactor).toInt()
            val targetHeight = (height * scaleFactor).toInt()

            // 2. Escalar (Usando KTX)
            val scaledBitmap = rotatedBitmap.scale(targetWidth, targetHeight, filter = true)

            // 3. Comprimir
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

            // Limpieza
            if (scaledBitmap != rotatedBitmap) scaledBitmap.recycle()
            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            originalBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(selectedImage) ?: return img
        val ei = ExifInterface(input)

        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}