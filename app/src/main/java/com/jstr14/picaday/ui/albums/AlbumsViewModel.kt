package com.jstr14.picaday.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
) : ViewModel() {

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            albumRepository.getAlbumsForUser()
                .catch { e -> e.printStackTrace() }
                .collect { list ->
                    _albums.value = list
                    _isLoading.value = false
                }
        }
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            try {
                albumRepository.createAlbum(name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
