package com.lovely.bakingrecipes.auth

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Wraps Firebase Auth for email/password and Google sign-in.
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    // Emits the signed-in user (or null) whenever auth state changes.
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun registerWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Unit
    }

    suspend fun signInWithFacebook(accessToken: String): Result<Unit> = runCatching {
        val credential = FacebookAuthProvider.getCredential(accessToken)
        auth.signInWithCredential(credential).await()
        Unit
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val request = UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        user.updateProfile(request).await()
        user.reload().await()
        Unit
    }

    // Uploads the picked image to Storage and points the profile photo at it.
    suspend fun updateProfilePhoto(uri: Uri): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val ref = FirebaseStorage.getInstance().reference.child("users/${user.uid}/profile.jpg")
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await()
        val request = UserProfileChangeRequest.Builder().setPhotoUri(downloadUrl).build()
        user.updateProfile(request).await()
        user.reload().await()
        Unit
    }

    // Sends a verification link to the new address; the change applies after the user confirms.
    suspend fun updateEmail(email: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        user.verifyBeforeUpdateEmail(email.trim()).await()
        Unit
    }

    suspend fun reloadUser(): Result<Unit> = runCatching {
        auth.currentUser?.reload()?.await()
        Unit
    }

    fun signOut() {
        auth.signOut()
    }
}
