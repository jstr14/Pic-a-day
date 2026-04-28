package com.jstr14.picaday.ui.daydetail

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.jstr14.picaday.data.repository.FirebaseStorageRepository
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.data.repository.ImageRepository
import com.jstr14.picaday.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageRepository: ImageRepository,
    private val storageRepository: FirebaseStorageRepository,
    private val processImageUseCase: ProcessImageUseCase,
) : ViewModel() {

    private val imageLoader by lazy { ImageLoader(context) }

    private val _saveToGalleryResult = MutableStateFlow<SaveResult?>(null)
    val saveToGalleryResult: StateFlow<SaveResult?> = _saveToGalleryResult.asStateFlow()

    private val _state = MutableStateFlow<DayEntry?>(null)
    val state: StateFlow<DayEntry?> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private var activeUploads = 0

    fun loadData(dateString: String) {
        val date = LocalDate.parse(dateString)
        val month = YearMonth.from(date)

        viewModelScope.launch {
            _isLoading.value = true
            imageRepository.getEntriesForMonth(month).collect { entries ->
                _state.value = entries.find { it.date == date }
                _isLoading.value = false
            }
        }
    }

    fun addMultiplePhotosToSpecificDay(date: LocalDate, uris: List<Uri>) {
        viewModelScope.launch {
            activeUploads += uris.size
            _isUploading.value = true
            uris.forEach { uri ->
                launch(Dispatchers.IO) {
                    try {
                        val processed = processImageUseCase(uri) ?: return@launch
                        val url = withContext(Dispatchers.IO) {
                            storageRepository.uploadPhoto(processed.bytes)
                        }
                        if (url != null) {
                            imageRepository.addPhotoToDate(date, url, processed.time, processed.lat, processed.lon)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        activeUploads--
                        if (activeUploads <= 0) {
                            activeUploads = 0
                            _isUploading.value = false
                            // If we uploaded from the empty state the cache-backed flow already
                            // completed — reload so a live Firestore listener is established.
                            if (_state.value == null) loadData(date.toString())
                        }
                    }
                }
            }
        }
    }

    fun deletePhoto(date: LocalDate, urlToDelete: String) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                val wasDeleted = storageRepository.deletePhoto(urlToDelete)
                if (wasDeleted) {
                    val updatedPhotos = _state.value?.photos?.filter { it.url != urlToDelete } ?: emptyList()
                    if (updatedPhotos.isEmpty()) {
                        imageRepository.deleteDayEntry(date)
                        _state.value = null
                    } else {
                        imageRepository.updatePhotos(date, updatedPhotos)
                    }
                }
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun deleteFullDay(date: LocalDate, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            val currentEntry = _state.value ?: run { _isDeleting.value = false; return@launch }
            try {
                if (currentEntry.imageUrls.isNotEmpty()) {
                    storageRepository.deleteMultiplePhotos(currentEntry.imageUrls)
                }
                imageRepository.deleteDayEntry(date)
                _state.value = null
            } finally {
                _isDeleting.value = false
                onComplete()
            }
        }
    }

    fun saveImageToGallery(imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val bitmap = (imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
                    ?: run { _saveToGalleryResult.value = SaveResult.Error; return@launch }

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
                    ?: run { _saveToGalleryResult.value = SaveResult.Error; return@launch }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                _saveToGalleryResult.value = SaveResult.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _saveToGalleryResult.value = SaveResult.Error
            }
        }
    }

    fun updateDescription(date: LocalDate, description: String) {
        _state.value = _state.value?.copy(description = description.ifBlank { null })
        viewModelScope.launch {
            imageRepository.updateDescription(date, description)
        }
    }

    fun clearSaveResult() {
        _saveToGalleryResult.value = null
    }
}

sealed class SaveResult {
    object Success : SaveResult()
    object Error : SaveResult()
}