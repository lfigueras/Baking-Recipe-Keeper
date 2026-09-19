package com.lovely.bakingrecipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.repository.PastryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PastryListViewModel(
    repository: PastryRepository,
    private val category: String?,
    private val favoritesOnly: Boolean = false
) : ViewModel() {

    val title: String = when {
        favoritesOnly -> "Favorites"
        category != null -> category
        else -> "All Recipes"
    }

    val isLoading: StateFlow<Boolean> =
        repository.allPastries.map { false }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val pastries: StateFlow<List<PastryWithIngredients>> =
        repository.allPastries
            .map { pastries ->
                pastries
                    .filter { category == null || it.pastry.category == category }
                    .filter { !favoritesOnly || it.pastry.isFavorite }
                    .sortedBy { it.pastry.name.lowercase() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
