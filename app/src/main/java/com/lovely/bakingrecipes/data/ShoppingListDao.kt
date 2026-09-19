package com.lovely.bakingrecipes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_list_items ORDER BY isChecked ASC, createdAt ASC")
    fun getAll(): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingListItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingListItem>)

    @Update
    suspend fun update(item: ShoppingListItem)

    @Query("UPDATE shopping_list_items SET isChecked = :checked WHERE id = :id")
    suspend fun setChecked(id: Int, checked: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM shopping_list_items WHERE isChecked = 1")
    suspend fun clearChecked()

    @Query("DELETE FROM shopping_list_items")
    suspend fun clearAll()
}
