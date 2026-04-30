package com.jstr14.picaday.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jstr14.picaday.domain.model.User
import com.jstr14.picaday.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toUser())
    override val currentUser = _currentUser.asStateFlow()

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Error retrieving the user data")
            val user = firebaseUser.toUser()
            _currentUser.value = user
            writeUserProfile(firebaseUser)
            claimPendingInvites(firebaseUser)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun writeUserProfile(firebaseUser: FirebaseUser) {
        val profile = buildMap<String, Any?> {
            firebaseUser.email?.let { put("email", it) }
            firebaseUser.displayName?.let { put("displayName", it) }
            firebaseUser.photoUrl?.toString()?.let { put("photoUrl", it) }
        }
        if (profile.isEmpty()) return
        try {
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(profile, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun claimPendingInvites(firebaseUser: FirebaseUser) {
        val email = firebaseUser.email ?: return
        try {
            val albums = firestore.collection("albums")
                .whereArrayContains("pendingInvites", email)
                .get()
                .await()
            for (doc in albums.documents) {
                firestore.collection("albums").document(doc.id)
                    .update(
                        mapOf(
                            "pendingInvites" to FieldValue.arrayRemove(email),
                            "memberIds" to FieldValue.arrayUnion(firebaseUser.uid),
                            "members.${firebaseUser.uid}" to "member",
                            "memberEmails.${firebaseUser.uid}" to email,
                            "memberDisplayNames.${firebaseUser.uid}" to (firebaseUser.displayName ?: ""),
                            "memberPhotoUrls.${firebaseUser.uid}" to (firebaseUser.photoUrl?.toString() ?: ""),
                        )
                    )
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    private fun FirebaseUser.toUser() = User(
        id = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )
}