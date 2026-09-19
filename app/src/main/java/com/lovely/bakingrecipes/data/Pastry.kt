package com.lovely.bakingrecipes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pastries")
data class Pastry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val category: String,
    val description: String,
    val imageUri: String?,
    val servings: Int = 0,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val difficulty: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)