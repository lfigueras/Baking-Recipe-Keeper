package com.lovely.bakingrecipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovely.bakingrecipes.data.IngredientUnit
import com.lovely.bakingrecipes.data.ShoppingListItem
import com.lovely.bakingrecipes.repository.PastryRepository
import com.lovely.bakingrecipes.repository.ShoppingListRepository
import com.lovely.bakingrecipes.util.Analytics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val repository: ShoppingListRepository,
    private val pastryRepository: PastryRepository
) : ViewModel() {

    val items: StateFlow<List<ShoppingListItem>> =
        repository.items.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addManual(name: String, amount: Double, unit: IngredientUnit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addManual(name, amount, unit)
            Analytics.shoppingListAdd("manual")
        }
    }

    // Pulls every recipe's ingredients into the list, unchecked, for a kitchen-inventory pass.
    fun addAllRecipeIngredients() {
        viewModelScope.launch {
            val recipes = pastryRepository.getAllOnce()
            repository.addFromAllRecipes(recipes)
            Analytics.shoppingListAdd("all_recipes")
        }
    }

    fun setChecked(id: Int, checked: Boolean) {
        viewModelScope.launch { repository.setChecked(id, checked) }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearChecked() {
        viewModelScope.launch { repository.clearChecked() }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
