package com.lovely.bakingrecipes.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.lovely.bakingrecipes.auth.AuthRepository
import com.lovely.bakingrecipes.util.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow(repository.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    init {
        viewModelScope.launch {
            repository.authState.collect { _user.value = it }
        }
    }

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun clearMessage() {
        _message.value = null
    }

    fun signInWithEmail(email: String, password: String) {
        if (!validate(email, password)) return
        run("Signed in") { repository.signInWithEmail(email, password) }
    }

    fun registerWithEmail(email: String, password: String) {
        if (!validate(email, password)) return
        run("Account created") { repository.registerWithEmail(email, password) }
    }

    fun signInWithGoogle(idToken: String) {
        run("Signed in with Google") { repository.signInWithGoogle(idToken) }
    }

    fun signInWithFacebook(accessToken: String) {
        run("Signed in with Facebook") { repository.signInWithFacebook(accessToken) }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _message.value = "Enter your email first"
            return
        }
        run("Password reset email sent") { repository.sendPasswordReset(email) }
    }

    fun updateDisplayName(name: String) {
        if (name.isBlank()) {
            _message.value = "Name can't be empty"
            return
        }
        runAndRefresh("Name updated") { repository.updateDisplayName(name) }
    }

    fun updateProfilePhoto(uri: Uri) {
        runAndRefresh("Profile photo updated") { repository.updateProfilePhoto(uri) }
    }

    fun updateEmail(email: String) {
        if (email.isBlank()) {
            _message.value = "Enter an email"
            return
        }
        run("Verification sent to $email. Confirm it to finish the change.") {
            repository.updateEmail(email)
        }
    }

    fun signOut() {
        repository.signOut()
        _message.value = "Signed out"
    }

    private fun validate(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _message.value = "Enter your email"
            return false
        }
        if (password.length < 6) {
            _message.value = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun run(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            result
                .onSuccess {
                    _message.value = successMessage
                    Analytics.authEvent("sign_in")
                }
                .onFailure { _message.value = it.message ?: "Something went wrong" }
        }
    }

    // Like run(), but refreshes the cached user so profile edits show immediately.
    private fun runAndRefresh(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            if (result.isSuccess) {
                repository.reloadUser()
                _user.value = repository.currentUser
            }
            _isBusy.value = false
            result
                .onSuccess { _message.value = successMessage }
                .onFailure { _message.value = it.message ?: "Something went wrong" }
        }
    }
}
