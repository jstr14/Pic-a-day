package com.jstr14.picaday.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jstr14.picaday.data.model.DayEntryDto
import com.jstr14.picaday.data.model.toDomain
import com.jstr14.picaday.data.model.toDto
import com.jstr14.picaday.domain.model.DayEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class FirebaseImageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ImageRepository {

    // current userId
    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getEntriesForMonth(month: YearMonth): Flow<List<DayEntry>> {
        val currentUid = userId ?: return flowOf(emptyList())

        // Mapping YearMonth object to string in the format "yyyy-MM" for the query
        val monthQueryString = "${month.year}-${month.monthValue.toString().padStart(2, '0')}"

        return callbackFlow {
            val query = firestore.collection("users")
                .document(currentUid) //
                .collection("entries")
                .whereEqualTo("yearMonth", monthQueryString)

            val subscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DayEntryDto::class.java)?.toDomain()
                } ?: emptyList()

                trySend(entries)
            }
            awaitClose { subscription.remove() }
        }
    }

    override suspend fun saveDayEntry(dayEntry: DayEntry) {
        val uid = userId ?: return

        val dto = dayEntry.toDto()

        try {
            // users/{userId}/entries/{date}
            // use the date (example: "2026-04-15") as document ID is the key
            // to edit the same day and overwrite it
            firestore.collection("users")
                .document(uid)
                .collection("entries")
                .document(dto.date)
                .set(dto) // save or overwrite the document
                .await()

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun updateImageUrls(date: LocalDate, newUrls: List<String>) {
        val uid = userId ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("entries")
                .document(date.toString())
                .update("imageUrls", newUrls)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteDayEntry(date: LocalDate) {
        val uid = userId ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("entries")
                .document(date.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun addPhotoToDate(date: LocalDate, imageUrl: String) {
        val uid = userId ?: return
        val dateStr = date.toString()
        val yearMonth = "${date.year}-${date.monthValue.toString().padStart(2, '0')}"

        val docRef = firestore.collection("users").document(uid)
            .collection("entries").document(dateStr)

        val data = mapOf(
            "date" to dateStr,
            "yearMonth" to yearMonth,
            "imageUrls" to FieldValue.arrayUnion(imageUrl) // ESTO es la clave para multi-subida
        )

        // set con merge: true para que no borre la descripción si ya existía
        docRef.set(data, SetOptions.merge()).await()
    }
}