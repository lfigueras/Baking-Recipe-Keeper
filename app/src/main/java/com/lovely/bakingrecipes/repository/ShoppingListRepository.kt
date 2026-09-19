package com.lovely.bakingrecipes.repository

import com.lovely.bakingrecipes.data.IngredientUnit
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.ShoppingListDao
import com.lovely.bakingrecipes.data.ShoppingListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao
) {

    val items: Flow<List<ShoppingListItem>> = shoppingListDao.getAll()

    suspend fun addManual(name: String, amount: Double, unit: IngredientUnit) {
        shoppingListDao.insert(
            ShoppingListItem(name = name.trim(), amount = amount, unit = unit)
        )
    }

    // Aggregates a recipe's ingredients (scaled) into the list, merging matching name+unit.
    suspend fun addFromRecipe(recipe: PastryWithIngredients, scale: Double) {
        val existing = mergeKeyMap()
        recipe.ingredients.forEach { ing ->
            val key = keyOf(ing.name, ing.unit)
            val scaledAmount = ing.amount * scale
            val match = existing[key]
            if (match != null) {
                shoppingListDao.update(match.copy(amount = match.amount + scaledAmount))
            } else {
                shoppingListDao.insert(
                    ShoppingListItem(
                        name = ing.name.trim(),
                        amount = scaledAmount,
                        unit = ing.unit,
                        sourcePastryId = recipe.pastry.id
                    )
                )
            }
        }
    }

    // Aggregates every recipe's ingredients (de-duplicated by name + unit) into the list, unchecked.
    // Returns how many distinct ingredient lines were added or merged.
    suspend fun addFromAllRecipes(recipes: List<PastryWithIngredients>): Int {
        val incoming = LinkedHashMap<String, ShoppingListItem>()
        recipes.forEach { recipe ->
            recipe.ingredients.forEach { ing ->
                if (ing.name.isBlank()) return@forEach
                val key = keyOf(ing.name, ing.unit)
                val current = incoming[key]
                incoming[key] = current?.copy(amount = current.amount + ing.amount)
                    ?: ShoppingListItem(
                        name = ing.name.trim(),
                        amount = ing.amount,
                        unit = ing.unit,
                        isChecked = false
                    )
            }
        }
        val existing = mergeKeyMap()
        incoming.forEach { (key, item) ->
            val match = existing[key]
            if (match != null) {
                shoppingListDao.update(match.copy(amount = match.amount + item.amount))
            } else {
                shoppingListDao.insert(item)
            }
        }
        return incoming.size
    }

    suspend fun setChecked(id: Int, checked: Boolean) = shoppingListDao.setChecked(id, checked)

    suspend fun delete(id: Int) = shoppingListDao.deleteById(id)

    suspend fun clearChecked() = shoppingListDao.clearChecked()

    suspend fun clearAll() = shoppingListDao.clearAll()

    private suspend fun mergeKeyMap(): Map<String, ShoppingListItem> {
        val current = shoppingListDao.getAll().first()
        return current.associateBy { keyOf(it.name, it.unit) }
    }

    private fun keyOf(name: String, unit: IngredientUnit): String =
        "${name.trim().lowercase()}|${unit.name}"
}
