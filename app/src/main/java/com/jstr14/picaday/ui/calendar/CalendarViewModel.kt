package com.jstr14.picaday.ui.calendar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.data.repository.FirebaseStorageRepository
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.data.repository.ImageRepository
import com.jstr14.picaday.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val storageRepository: FirebaseStorageRepository,
    private val processImageUseCase: ProcessImageUseCase,
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DayEntry>>(emptyList())
    val entries: StateFlow<List<DayEntry>> = _entries.asStateFlow()

    private val _yearEntryDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val yearEntryDates: StateFlow<Set<LocalDate>> = _yearEntryDates.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    fun updateVisibleMonth(month: YearMonth) {
        _visibleMonth.value = month
    }

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _scrollToToday = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToToday: SharedFlow<Unit> = _scrollToToday.asSharedFlow()

    fun requestScrollToToday() {
        _scrollToToday.tryEmit(Unit)
    }

    private var activeUploads = 0
    /**
     * Loads images for the given month.
     */
    fun loadMonthData(month: YearMonth) {
        viewModelScope.launch {
            imageRepository.getEntriesForMonth(month).collect { loadedEntries ->
                _entries.value = loadedEntries
            }
        }
    }

    fun loadYearData(year: Int) {
        viewModelScope.launch {
            imageRepository.getEntriesForYear(year).collect { loadedEntries ->
                _yearEntryDates.value = loadedEntries.map { it.date }.toSet()
            }
        }
    }

    fun uploadMultipleImages(uris: List<Uri>) {
        activeUploads += uris.size
        _isUploading.value = true
        uris.forEach { uri ->
            // Usamos viewModelScope para que si se cierra la pantalla,
            // las subidas en curso tengan oportunidad de terminar (o cancelarse adecuadamente)
            viewModelScope.launch {
                try {
                    // 1. Procesamiento (Compresión + EXIF) delegado al Use Case
                    // El Use Case ya corre en Dispatchers.IO internamente
                    val processed = processImageUseCase(uri) ?: return@launch

                    // 2. Subida a Storage (corriendo en segundo plano)
                    val url = withContext(Dispatchers.IO) {
                        storageRepository.uploadPhoto(processed.bytes)
                    }

                    if (url != null) {
                        // 3. Registro en Firestore en la fecha detectada
                        imageRepository.addPhotoToDate(processed.date, url)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Aquí podrías manejar un estado de error para la UI si fuera necesario
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
}