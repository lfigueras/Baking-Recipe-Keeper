package com.lovely.bakingrecipes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Double,
    val unit: IngredientUnit,
    val isChecked: Boolean = false,
    val sourcePastryId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
