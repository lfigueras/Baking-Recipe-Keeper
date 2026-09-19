package com.lovely.bakingrecipes.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lovely.bakingrecipes.repository.PastryRepository
import com.lovely.bakingrecipes.util.Analytics
import com.lovely.bakingrecipes.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupViewModel(
    application: Application,
    private val repository: PastryRepository
) : AndroidViewModel(application) {

    fun exportTo(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                runCatching {
                    val recipes = repository.getAllOnce()
                    val json = BackupManager.toJson(recipes)
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    }
                    "Exported ${recipes.size} recipes"
                }.getOrElse { "Export failed" }
            }
            Analytics.backup("export")
            onResult(message)
        }
    }

    fun importFrom(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                runCatching {
                    val json = getApplication<Application>().contentResolver
                        .openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: return@runCatching "Import failed"
                    val imported = BackupManager.fromJson(json)
                    imported.forEach {
                        repository.insertPastryWithIngredients(
                            it.pastry, it.ingredients, it.steps, it.tags
                        )
                    }
                    "Imported ${imported.size} recipes"
                }.getOrElse { "Import failed" }
            }
            Analytics.backup("import")
            onResult(message)
        }
    }
}
