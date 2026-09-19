package com.lovely.bakingrecipes.ui.onboarding

import android.content.Context

// Tracks whether the first-launch walkthrough has been completed.
object OnboardingPreferences {
    private const val PREFS = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    fun setCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .apply()
    }
}
