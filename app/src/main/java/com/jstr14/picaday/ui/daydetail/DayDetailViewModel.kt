package com.jstr14.picaday.ui.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DayEntry?>(null)
    val state: StateFlow<DayEntry?> = _state.asStateFlow()

    fun loadData(dateString: String) {
        val date = LocalDate.parse(dateString)
        val month = YearMonth.from(date)

        viewModelScope.launch {
            repository.getEntriesForMonth(month).collect { entries ->
                _state.value = entries.find { it.date == date }
            }
        }
    }
}