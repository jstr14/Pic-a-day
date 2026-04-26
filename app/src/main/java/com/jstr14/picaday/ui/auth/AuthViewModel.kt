package com.jstr14.picaday.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstr14.picaday.domain.model.User
import com.jstr14.picaday.domain.repository.AuthRepository
import com.jstr14.picaday.domain.repository.SessionClearable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionClearable: SessionClearable
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val currentUser = authRepository.currentUser
    private val _errorEvents = MutableStateFlow<String?>(null)
    val uiState: StateFlow<AuthState> = combine(
        authRepository.currentUser,
        _isLoading,
        _errorEvents,
    ) { user, loading, error ->
        when {
            error != null -> AuthState.Error(error)
            loading -> AuthState.Loading
            user != null -> AuthState.Success(user)
            else -> AuthState.Idle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Idle
    )

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _isInitializing.value = false
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _errorEvents.value = "Invalid Token"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorEvents.value = null
            val result = authRepository.signInWithGoogle(idToken)
            result.onFailure { exception ->
                _errorEvents.value = "Google Sign-In failed: ${exception.message}"
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        sessionClearable.clearSession()
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}