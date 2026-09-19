package com.lovely.bakingrecipes.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

// Central place for logging product analytics; initialized once at app start.
object Analytics {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
        }
    }

    fun appOpen() = log(FirebaseAnalytics.Event.APP_OPEN)

    fun screenView(screenName: String) {
        log(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }

    fun recipeCreated(category: String, ingredientCount: Int, stepCount: Int) {
        log("recipe_created", Bundle().apply {
            putString("category", category.ifBlank { "uncategorized" })
            putInt("ingredient_count", ingredientCount)
            putInt("step_count", stepCount)
        })
    }

    fun recipeEdited() = log("recipe_edited")

    fun recipeDeleted() = log("recipe_deleted")

    fun recipeDuplicated() = log("recipe_duplicated")

    fun recipeFavorited(favorite: Boolean) {
        log("recipe_favorited", Bundle().apply { putBoolean("favorite", favorite) })
    }

    fun recipeShared() = log("recipe_shared")

    fun recipeExported(format: String) {
        log("recipe_exported", Bundle().apply { putString("format", format) })
    }

    fun startBaking() = log("start_baking")

    fun shoppingListAdd(source: String) {
        log("shopping_list_add", Bundle().apply { putString("source", source) })
    }

    fun backup(action: String) {
        log("backup", Bundle().apply { putString("action", action) })
    }

    fun authEvent(action: String) {
        log("auth", Bundle().apply { putString("action", action) })
    }

    private fun log(name: String, params: Bundle? = null) {
        firebaseAnalytics?.logEvent(name, params)
    }
}
