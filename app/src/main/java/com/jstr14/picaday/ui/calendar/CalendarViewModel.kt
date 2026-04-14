package com.jstr14.picaday.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DayEntry>>(emptyList())
    val entries: StateFlow<List<DayEntry>> = _entries.asStateFlow()

    /**
     * Loads images for the given month.
     */
    fun loadMonthData(month: YearMonth) {
        viewModelScope.launch {
            repository.getEntriesForMonth(month).collect { loadedEntries ->
                _entries.value = loadedEntries
            }
        }
    }
}