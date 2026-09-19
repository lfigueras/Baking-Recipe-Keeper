package com.lovely.bakingrecipes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovely.bakingrecipes.auth.AuthRepository
import com.lovely.bakingrecipes.navigation.AppNavigation
import com.lovely.bakingrecipes.ui.onboarding.OnboardingPreferences
import com.lovely.bakingrecipes.ui.onboarding.OnboardingScreen
import com.lovely.bakingrecipes.ui.screens.account.LoginScreen
import com.lovely.bakingrecipes.ui.theme.BakingRecipesTheme
import com.lovely.bakingrecipes.ui.theme.ThemeMode
import com.lovely.bakingrecipes.ui.theme.ThemePreferences
import com.lovely.bakingrecipes.util.Analytics
import com.lovely.bakingrecipes.viewmodel.AuthViewModel
import com.lovely.bakingrecipes.viewmodel.GenericViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializes Analytics and records an app-open so events start flowing to Firebase.
        Analytics.init(this)
        Analytics.appOpen()

        setContent {
            val context = LocalContext.current
            var themeMode by rememberSaveable { mutableStateOf(ThemePreferences.load(context)) }
            var onboardingDone by rememberSaveable {
                mutableStateOf(OnboardingPreferences.isCompleted(context))
            }
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            BakingRecipesTheme(darkTheme = darkTheme) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = GenericViewModelFactory { AuthViewModel(AuthRepository()) }
                )
                // Non-null when a session is cached, so signed-in users pass the gate even offline.
                val user by authViewModel.user.collectAsState()
                val isBusy by authViewModel.isBusy.collectAsState()
                val message by authViewModel.message.collectAsState()

                when {
                    !onboardingDone -> OnboardingScreen(
                        onFinished = {
                            OnboardingPreferences.setCompleted(context)
                            onboardingDone = true
                        }
                    )

                    user == null -> LoginScreen(
                        isBusy = isBusy,
                        message = message,
                        onMessageShown = authViewModel::clearMessage,
                        onSignInEmail = authViewModel::signInWithEmail,
                        onRegisterEmail = authViewModel::registerWithEmail,
                        onGoogleIdToken = authViewModel::signInWithGoogle,
                        onFacebookToken = authViewModel::signInWithFacebook,
                        onForgotPassword = authViewModel::sendPasswordReset
                    )

                    else -> AppNavigation(
                        themeMode = themeMode,
                        onThemeModeChange = {
                            themeMode = it
                            ThemePreferences.save(context, it)
                        }
                    )
                }
            }
        }
    }
}