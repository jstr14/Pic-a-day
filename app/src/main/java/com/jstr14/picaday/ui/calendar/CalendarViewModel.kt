package com.jstr14.picaday.ui.calendar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.data.repository.FirebaseStorageRepository
import com.jstr14.picaday.data.repository.ImageRepository
import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.repository.AlbumRepository
import com.jstr14.picaday.domain.usecase.GetMergedEntriesUseCase
import com.jstr14.picaday.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val albumRepository: AlbumRepository,
    private val getMergedEntries: GetMergedEntriesUseCase,
    private val storageRepository: FirebaseStorageRepository,
    private val processImageUseCase: ProcessImageUseCase,
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DayEntry>>(emptyList())
    val entries: StateFlow<List<DayEntry>> = _entries.asStateFlow()

    private val _yearEntryDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val yearEntryDates: StateFlow<Set<LocalDate>> = _yearEntryDates.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    private val _isYearMode = MutableStateFlow(false)
    val isYearMode: StateFlow<Boolean> = _isYearMode.asStateFlow()

    fun setYearMode(enabled: Boolean) {
        _isYearMode.value = enabled
    }

    val albums: StateFlow<List<Album>> = albumRepository.getAlbumsForUser()
        .catch { e -> e.printStackTrace() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun loadMonthData(month: YearMonth) {
        viewModelScope.launch {
            getMergedEntries.forMonth(month)
                .catch { e -> e.printStackTrace() }
                .collect { loadedEntries ->
                    _entries.value = loadedEntries
                }
        }
    }

    fun loadYearData(year: Int) {
        viewModelScope.launch {
            getMergedEntries.forYear(year)
                .catch { e -> e.printStackTrace() }
                .collect { loadedEntries ->
                    _yearEntryDates.value = loadedEntries.map { it.date }.toSet()
                }
        }
    }

    /**
     * [albumId] null → personal diary; non-null → upload to that shared album.
     */
    fun uploadMultipleImages(uris: List<Uri>, albumId: String? = null) {
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
                        if (albumId == null) {
                            imageRepository.addPhotoToDate(
                                processed.date, url, processed.time, processed.lat, processed.lon
                            )
                        } else {
                            albumRepository.addPhotoToAlbumEntry(
                                albumId, processed.date, url, processed.time, processed.lat, processed.lon
                            )
                        }
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
}
