package com.lovely.bakingrecipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.repository.PastryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val pastries: List<PastryWithIngredients> = emptyList(),
    val totalPastries: Int = 0,
    val totalIngredients: Int = 0,
    val categoryCount: Int = 0,
    val categories: List<String> = emptyList(),
    val tiles: List<DashboardTile> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null
)

class HomeViewModel(
    repository: PastryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.allPastries,
            searchQuery,
            selectedCategory
        ) { all, query, category ->
            val categories = all
                .map { it.pastry.category }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val filtered = all.filter { item ->
                val matchesQuery = query.isBlank() || item.matchesSearch(query)
                val matchesCategory = category == null || item.pastry.category == category
                matchesQuery && matchesCategory
            }

            HomeUiState(
                isLoading = false,
                pastries = filtered,
                totalPastries = all.size,
                totalIngredients = all.sumOf { it.ingredients.size },
                categoryCount = categories.size,
                categories = categories,
                tiles = buildDashboardTiles(all),
                searchQuery = query,
                selectedCategory = category
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }

    // Tapping the active category clears the filter.
    fun onCategorySelected(category: String?) {
        selectedCategory.value = if (selectedCategory.value == category) null else category
    }
}

// Matches a query against the recipe name, category, description, tags, and ingredient names.
private fun PastryWithIngredients.matchesSearch(query: String): Boolean {
    val q = query.trim()
    if (q.isBlank()) return true
    if (pastry.name.contains(q, ignoreCase = true)) return true
    if (pastry.category.contains(q, ignoreCase = true)) return true
    if (pastry.description.contains(q, ignoreCase = true)) return true
    if (tags.any { it.name.contains(q, ignoreCase = true) }) return true
    if (ingredients.any { it.name.contains(q, ignoreCase = true) }) return true
    return false
}