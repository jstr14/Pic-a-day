package com.jstr14.picaday.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState = _uiState.asStateFlow()
    val currentUser = authRepository.currentUser

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _isInitializing.value = false
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { _uiState.value = AuthState.Success },
                onFailure = { _uiState.value = AuthState.Error("Google Sign-In failed") }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthState.Idle // Reseteamos el estado al estado inicial
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}