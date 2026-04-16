package com.jstr14.picaday.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    private val userId: String? get() = auth.currentUser?.uid

    suspend fun uploadPhoto(bytes: ByteArray): String? {
        val uid = userId ?: return null

        // Generamos un nombre único para evitar colisiones en subidas múltiples
        val fileName = "photo_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"

        val photoRef = storage.reference
            .child("users")
            .child(uid)
            .child("photos")
            .child(fileName)

        return try {
            // 1. Usamos putBytes en lugar de putFile
            photoRef.putBytes(bytes).await()

            // 2. Obtenemos la URL de descarga
            val downloadUrl = photoRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            // Es buena práctica loguear el error específico para debug
            println("Error subiendo foto: ${e.message}")
            null
        }
    }
    suspend fun uploadPhoto(uri: Uri): String? {
        val uid = userId ?: return null

        val fileName = "photo_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
        val photoRef = storage.reference
            .child("users")
            .child(uid)
            .child("photos")
            .child(fileName)

        return try {
            // Subimos el archivo y esperamos a que termine (.await())
            photoRef.putFile(uri).await()

            // Una vez subido, pedimos la URL para poder mostrarla con Coil
            val downloadUrl = photoRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deletePhoto(photoUrl: String): Boolean {
        return try {
            val photoRef = storage.getReferenceFromUrl(photoUrl)
            photoRef.delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteMultiplePhotos(urls: List<String>) {
        urls.forEach { url ->
            try {
                val photoRef = storage.getReferenceFromUrl(url)
                photoRef.delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}