package com.jstr14.picaday.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jstr14.picaday.data.model.AlbumDto
import com.jstr14.picaday.data.model.DayEntryDto
import com.jstr14.picaday.data.model.toDomain
import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.model.AlbumRole
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.repository.AlbumRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class FirebaseAlbumRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : AlbumRepository {

    private val userId: String? get() = auth.currentUser?.uid

    override fun getAlbumsForUser(): Flow<List<Album>> {
        val uid = userId ?: return flowOf(emptyList())
        return callbackFlow {
            val sub = firestore.collection("albums")
                .whereArrayContains("memberIds", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    val albums = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(AlbumDto::class.java)?.copy(id = doc.id)?.toDomain()
                    } ?: emptyList()
                    trySend(albums)
                }
            awaitClose { sub.remove() }
        }
    }

    override fun getAlbum(albumId: String): Flow<Album?> {
        return callbackFlow {
            val sub = firestore.collection("albums").document(albumId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    val album = snapshot?.toObject(AlbumDto::class.java)
                        ?.copy(id = snapshot.id)
                        ?.toDomain()
                    trySend(album)
                }
            awaitClose { sub.remove() }
        }
    }

    override fun getEntriesForMonth(albumId: String, month: YearMonth): Flow<List<DayEntry>> {
        val monthStr = "${month.year}-${month.monthValue.toString().padStart(2, '0')}"
        return callbackFlow {
            val sub = firestore.collection("albums").document(albumId)
                .collection("entries")
                .whereEqualTo("yearMonth", monthStr)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    val entries = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(DayEntryDto::class.java)?.toDomain()
                    } ?: emptyList()
                    trySend(entries)
                }
            awaitClose { sub.remove() }
        }
    }

    override fun getEntriesForYear(albumId: String, year: Int): Flow<List<DayEntry>> {
        return callbackFlow {
            val sub = firestore.collection("albums").document(albumId)
                .collection("entries")
                .whereGreaterThanOrEqualTo("yearMonth", "$year-01")
                .whereLessThanOrEqualTo("yearMonth", "$year-12")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    val entries = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(DayEntryDto::class.java)?.toDomain()
                    } ?: emptyList()
                    trySend(entries)
                }
            awaitClose { sub.remove() }
        }
    }

    override suspend fun createAlbum(name: String): Album {
        val uid = userId ?: throw IllegalStateException("Not authenticated")
        val ownerEmail = auth.currentUser?.email ?: ""
        val ownerName = auth.currentUser?.displayName ?: ""

        val ownerPhotoUrl = auth.currentUser?.photoUrl?.toString() ?: ""

        val albumData = mapOf(
            "name" to name,
            "ownerId" to uid,
            "memberIds" to listOf(uid),
            "members" to mapOf(uid to "owner"),
            "memberEmails" to mapOf(uid to ownerEmail),
            "memberDisplayNames" to mapOf(uid to ownerName),
            "memberPhotoUrls" to mapOf(uid to ownerPhotoUrl),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        val ref = firestore.collection("albums").add(albumData).await()
        return Album(
            id = ref.id,
            name = name,
            ownerId = uid,
            members = listOf(
                com.jstr14.picaday.domain.model.AlbumMember(
                    userId = uid,
                    role = AlbumRole.OWNER,
                    email = ownerEmail.ifBlank { null },
                    displayName = ownerName.ifBlank { null },
                )
            ),
        )
    }

    override suspend fun inviteMemberByEmail(albumId: String, email: String): Result<Boolean> {
        return try {
            val userQuery = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            val userDoc = userQuery.documents.firstOrNull()

            if (userDoc == null) {
                // User not registered yet — store email as a pending invite.
                // AuthRepositoryImpl will claim it automatically on their first sign-in.
                firestore.collection("albums").document(albumId)
                    .update("pendingInvites", FieldValue.arrayUnion(email))
                    .await()
                return Result.success(false)
            }

            val targetUserId = userDoc.id
            val targetDisplayName = userDoc.getString("displayName") ?: ""
            val targetPhotoUrl = userDoc.getString("photoUrl") ?: ""

            firestore.collection("albums").document(albumId)
                .update(
                    mapOf(
                        "memberIds" to FieldValue.arrayUnion(targetUserId),
                        "members.$targetUserId" to "member",
                        "memberEmails.$targetUserId" to email,
                        "memberDisplayNames.$targetUserId" to targetDisplayName,
                        "memberPhotoUrls.$targetUserId" to targetPhotoUrl,
                    )
                )
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(albumId: String, userId: String) {
        firestore.collection("albums").document(albumId)
            .update(
                mapOf(
                    "memberIds" to FieldValue.arrayRemove(userId),
                    "members.$userId" to FieldValue.delete(),
                    "memberEmails.$userId" to FieldValue.delete(),
                    "memberDisplayNames.$userId" to FieldValue.delete(),
                )
            )
            .await()
    }

    override fun getAllEntriesFlow(albumId: String): Flow<List<DayEntry>> = callbackFlow {
        val sub = firestore.collection("albums").document(albumId)
            .collection("entries")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DayEntryDto::class.java)?.toDomain()
                } ?: emptyList()
                trySend(entries)
            }
        awaitClose { sub.remove() }
    }

    override suspend fun getAllEntries(albumId: String): List<DayEntry> {
        val snapshot = firestore.collection("albums").document(albumId)
            .collection("entries").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(DayEntryDto::class.java)?.toDomain()
        }
    }

    override suspend fun deletePhotoFromAlbum(albumId: String, date: LocalDate, photoUrl: String) {
        val entryRef = firestore.collection("albums").document(albumId)
            .collection("entries").document(date.toString())
        val snapshot = entryRef.get().await()
        val dto = snapshot.toObject(DayEntryDto::class.java) ?: return
        val updatedPhotos = dto.photos.filter { it.url != photoUrl }
        if (updatedPhotos.isEmpty()) {
            entryRef.delete().await()
        } else {
            entryRef.update("photos", updatedPhotos).await()
        }
    }

    override suspend fun renameAlbum(albumId: String, newName: String) {
        firestore.collection("albums").document(albumId)
            .update("name", newName).await()
    }

    override suspend fun deleteAlbum(albumId: String) {
        // Delete all entries in the subcollection first, then the album document
        val entriesSnapshot = firestore.collection("albums").document(albumId)
            .collection("entries").get().await()
        val batch = firestore.batch()
        entriesSnapshot.documents.forEach { batch.delete(it.reference) }
        batch.delete(firestore.collection("albums").document(albumId))
        batch.commit().await()
    }

    override suspend fun getMemberPhotoUrls(memberIds: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        memberIds.forEach { uid ->
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                doc.getString("photoUrl")?.takeIf { it.isNotBlank() }?.let { result[uid] = it }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    override suspend fun addPhotoToAlbumEntry(
        albumId: String,
        date: LocalDate,
        imageUrl: String,
        time: String?,
        lat: Double?,
        lon: Double?,
    ) {
        val uid = userId ?: return
        val dateStr = date.toString()
        val yearMonth = "${date.year}-${date.monthValue.toString().padStart(2, '0')}"

        val photoMap = buildMap<String, Any?> {
            put("url", imageUrl)
            if (time != null) put("time", time)
            if (lat != null) put("lat", lat)
            if (lon != null) put("lon", lon)
            put("uploadedBy", uid)
            auth.currentUser?.displayName?.let { put("uploadedByName", it) }
        }

        val data = buildMap<String, Any> {
            put("date", dateStr)
            put("yearMonth", yearMonth)
            put("photos", FieldValue.arrayUnion(photoMap))
        }

        firestore.collection("albums").document(albumId)
            .collection("entries").document(dateStr)
            .set(data, SetOptions.merge())
            .await()
    }
}
