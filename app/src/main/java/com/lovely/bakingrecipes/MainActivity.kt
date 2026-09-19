package com.lovely.bakingrecipes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lovely.bakingrecipes.navigation.AppNavigation
import com.lovely.bakingrecipes.ui.onboarding.OnboardingPreferences
import com.lovely.bakingrecipes.ui.onboarding.OnboardingScreen
import com.lovely.bakingrecipes.ui.theme.BakingRecipesTheme
import com.lovely.bakingrecipes.ui.theme.ThemeMode
import com.lovely.bakingrecipes.ui.theme.ThemePreferences
import com.lovely.bakingrecipes.util.Analytics

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
                if (!onboardingDone) {
                    OnboardingScreen(
                        onFinished = {
                            OnboardingPreferences.setCompleted(context)
                            onboardingDone = true
                        }
                    )
                } else {
                    AppNavigation(
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