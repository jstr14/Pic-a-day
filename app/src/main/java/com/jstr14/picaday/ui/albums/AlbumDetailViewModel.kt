package com.jstr14.picaday.ui.albums

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.jstr14.picaday.data.repository.FirebaseStorageRepository
import com.jstr14.picaday.data.repository.ImageRepository
import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.repository.AlbumRepository
import com.jstr14.picaday.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import com.jstr14.picaday.domain.model.DayEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

sealed class InviteResult {
    object Success : InviteResult()
    object PendingInvite : InviteResult()
    object Error : InviteResult()
}

sealed class RemoveMemberError {
    object Member : RemoveMemberError()
    object PendingInvite : RemoveMemberError()
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val albumRepository: AlbumRepository,
    private val imageRepository: ImageRepository,
    private val storageRepository: FirebaseStorageRepository,
    private val processImageUseCase: ProcessImageUseCase,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"])
    val currentUserId: String? = auth.currentUser?.uid

    private val imageLoader by lazy { ImageLoader(context) }

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private val _isInviting = MutableStateFlow(false)
    val isInviting: StateFlow<Boolean> = _isInviting.asStateFlow()

    private val _inviteResult = MutableStateFlow<InviteResult?>(null)
    val inviteResult: StateFlow<InviteResult?> = _inviteResult.asStateFlow()

    private val _removeMemberError = MutableStateFlow<RemoveMemberError?>(null)
    val removeMemberError: StateFlow<RemoveMemberError?> = _removeMemberError.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _saveToGalleryResult = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val saveToGalleryResult: SharedFlow<Boolean> = _saveToGalleryResult.asSharedFlow()

    val entries: StateFlow<List<DayEntry>> = albumRepository.getAllEntriesFlow(albumId)
        .catch { e -> e.printStackTrace() }
        .map { it.sortedByDescending { entry -> entry.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var activeUploads = 0

    init {
        viewModelScope.launch {
            albumRepository.getAlbum(albumId)
                .catch { e -> e.printStackTrace() }
                .collect { album ->
                    _album.value = album?.let { enrichMemberPhotoUrls(it) }
                }
        }
    }

    private suspend fun enrichMemberPhotoUrls(album: Album): Album {
        val missing = album.members.filter { it.photoUrl.isNullOrBlank() }.map { it.userId }
        if (missing.isEmpty()) return album
        val fetched = albumRepository.getMemberPhotoUrls(missing)
        return album.copy(
            members = album.members.map { member ->
                if (member.photoUrl.isNullOrBlank()) member.copy(photoUrl = fetched[member.userId])
                else member
            }
        )
    }

    fun uploadPhotos(uris: List<Uri>) {
        activeUploads += uris.size
        _isUploading.value = true
        uris.forEach { uri ->
            viewModelScope.launch {
                try {
                    val processed = processImageUseCase(uri) ?: return@launch
                    val url = withContext(Dispatchers.IO) {
                        storageRepository.uploadPhoto(processed.bytes)
                    }
                    if (url != null) {
                        albumRepository.addPhotoToAlbumEntry(
                            albumId = albumId,
                            date = processed.date,
                            imageUrl = url,
                            time = processed.time,
                            lat = processed.lat,
                            lon = processed.lon,
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    activeUploads--
                    if (activeUploads <= 0) {
                        activeUploads = 0
                        _isUploading.value = false
                    }
                }
            }
        }
    }

    fun saveImageToGallery(imageUrl: String) = savePhotosToGallery(listOf(imageUrl))

    fun savePhotosToGallery(imageUrls: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = imageUrls.map { saveImageInternal(it) }
            _saveToGalleryResult.tryEmit(results.all { it })
        }
    }

    private suspend fun saveImageInternal(imageUrl: String): Boolean {
        return try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val bitmap = (imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
                ?: return false

            val filename = "PicADay_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PicADay")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun inviteByEmail(email: String) {
        viewModelScope.launch {
            _isInviting.value = true
            val result = albumRepository.inviteMemberByEmail(albumId, email)
            _inviteResult.value = when {
                result.isSuccess && result.getOrNull() == true -> InviteResult.Success
                result.isSuccess && result.getOrNull() == false -> InviteResult.PendingInvite
                else -> InviteResult.Error
            }
            _isInviting.value = false
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            try {
                albumRepository.removeMember(albumId, userId)
            } catch (e: Exception) {
                e.printStackTrace()
                _removeMemberError.value = RemoveMemberError.Member
            }
        }
    }

    fun removePendingInvite(email: String) {
        viewModelScope.launch {
            try {
                albumRepository.removePendingInvite(albumId, email)
            } catch (e: Exception) {
                e.printStackTrace()
                _removeMemberError.value = RemoveMemberError.PendingInvite
            }
        }
    }

    fun clearRemoveMemberError() {
        _removeMemberError.value = null
    }

    fun renameAlbum(newName: String) {
        viewModelScope.launch {
            try {
                albumRepository.renameAlbum(albumId, newName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun canDeletePhoto(uploadedByUid: String?): Boolean {
        val album = _album.value ?: return false
        return currentUserId == album.ownerId || currentUserId == uploadedByUid
    }

    fun deletePhoto(date: LocalDate, photoUrl: String) {
        viewModelScope.launch {
            try {
                albumRepository.deletePhotoFromAlbum(albumId, date, photoUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePhotos(photosByDate: Map<LocalDate, List<String>>) {
        viewModelScope.launch {
            photosByDate.entries.map { (date, urls) ->
                launch {
                    try {
                        urls.forEach { url ->
                            albumRepository.deletePhotoFromAlbum(albumId, date, url)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun clearInviteResult() {
        _inviteResult.value = null
    }

    fun deleteAlbum(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                albumRepository.deleteAlbum(albumId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }

    fun deleteAlbumAndMovePhotos(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val entries = albumRepository.getAllEntries(albumId)
                val existingUrlsByDate = mutableMapOf<LocalDate, Set<String>>()
                entries.forEach { entry ->
                    val existingUrls = existingUrlsByDate.getOrPut(entry.date) {
                        imageRepository.getEntry(entry.date)?.photos?.map { it.url }?.toSet()
                            ?: emptySet()
                    }
                    entry.photos
                        .filter { it.uploadedByUid == currentUserId || it.uploadedByUid == null }
                        .forEach { photo ->
                            if (photo.url !in existingUrls) {
                                imageRepository.addPhotoToDate(
                                    date = entry.date,
                                    imageUrl = photo.url,
                                    time = photo.time,
                                    lat = photo.lat,
                                    lon = photo.lon,
                                )
                            }
                        }
                }
                albumRepository.deleteAlbum(albumId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }
}
