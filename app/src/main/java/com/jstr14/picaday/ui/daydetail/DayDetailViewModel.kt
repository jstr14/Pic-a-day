package com.jstr14.picaday.ui.daydetail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.data.repository.FirebaseStorageRepository
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.data.repository.ImageRepository
import com.jstr14.picaday.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val imageRepository: ImageRepository,
    private val storageRepository: FirebaseStorageRepository,
    private val processImageUseCase: ProcessImageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DayEntry?>(null)
    val state: StateFlow<DayEntry?> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

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
                            // USAMOS addPhotoToDate:
                            // Asegura que se añadan al array sin sobreescribir las fotos que se
                            // están subiendo simultáneamente en otros hilos.
                            imageRepository.addPhotoToDate(date, url)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        // 5. IMPORTANTE: Decrementar SIEMPRE, ocurra error o no
                        activeUploads--
                        if (activeUploads <= 0) {
                            activeUploads = 0
                            _isUploading.value = false
                        }
                    }
                }
            }
        }
    }

    fun deletePhoto(date: LocalDate, urlToDelete: String) {
        viewModelScope.launch {
            val wasDeleted = storageRepository.deletePhoto(urlToDelete)

            if (wasDeleted) {
                val currentEntry = _state.value
                val currentUrls = currentEntry?.imageUrls ?: emptyList()
                val updatedUrls = currentUrls.filter { it != urlToDelete }

                // Deletion logic
                // If after filtering there aren't photos
                if (updatedUrls.isEmpty()) {
                    imageRepository.deleteDayEntry(date)
                    // Opcional: Podrías cerrar la pantalla o actualizar el estado a null
                    _state.value = null
                } else {
                    // Si aún quedan fotos, solo actualizamos la lista
                    imageRepository.updateImageUrls(date, updatedUrls)
                }
            }
        }
    }

    fun deleteFullDay(date: LocalDate, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentEntry = _state.value ?: return@launch

            // 1. Borramos todas las fotos del Storage físico
            if (currentEntry.imageUrls.isNotEmpty()) {
                storageRepository.deleteMultiplePhotos(currentEntry.imageUrls)
            }

            // 2. Borramos el documento de Firestore
            imageRepository.deleteDayEntry(date)

            // 3. Notificamos a la UI para que cierre la pantalla o limpie el estado
            _state.value = null
            onComplete() // Callback para navegar hacia atrás
        }
    }
}