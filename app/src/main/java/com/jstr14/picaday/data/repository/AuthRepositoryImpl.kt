package com.jstr14.picaday.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jstr14.picaday.domain.model.User
import com.jstr14.picaday.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toUser())
    override val currentUser = _currentUser.asStateFlow()

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user?.toUser() ?: throw Exception("Error retrieving the user data")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    private fun com.google.firebase.auth.FirebaseUser.toUser() = User(
        id = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )
}