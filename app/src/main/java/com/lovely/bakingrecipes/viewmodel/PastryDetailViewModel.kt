package com.lovely.bakingrecipes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lovely.bakingrecipes.data.ImageStorage
import com.lovely.bakingrecipes.data.Pastry
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.repository.PastryRepository
import com.lovely.bakingrecipes.util.Analytics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PastryDetailViewModel(
    application: Application,
    private val repository: PastryRepository,
    pastryId: Int
) : AndroidViewModel(application) {

    val pastry: StateFlow<PastryWithIngredients?> =
        repository.getPastryById(pastryId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun toggleFavorite() {
        val current = pastry.value ?: return
        viewModelScope.launch {
            val newValue = !current.pastry.isFavorite
            repository.setFavorite(current.pastry.id, newValue)
            Analytics.recipeFavorited(newValue)
        }
    }

    fun duplicate(onDuplicated: (Int) -> Unit) {
        val current = pastry.value ?: return
        viewModelScope.launch {
            val newId = repository.duplicatePastry(current)
            Analytics.recipeDuplicated()
            onDuplicated(newId)
        }
    }

    fun deletePastry(pastry: Pastry) {
        val media = this.pastry.value?.media.orEmpty()
        viewModelScope.launch {
            repository.deletePastry(pastry)
            ImageStorage.deleteIfLocal(getApplication(), pastry.imageUri)
            media.forEach { ImageStorage.deleteIfLocal(getApplication(), it.uri) }
            Analytics.recipeDeleted()
        }
    }
}
