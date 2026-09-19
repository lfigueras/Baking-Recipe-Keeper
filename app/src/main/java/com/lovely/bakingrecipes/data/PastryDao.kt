package com.lovely.bakingrecipes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PastryDao {

    @Insert
    suspend fun insertPastry(pastry: Pastry): Long

    @Update
    suspend fun updatePastry(pastry: Pastry)

    @Insert
    suspend fun insertIngredients(ingredients: List<Ingredient>)

    @Query("DELETE FROM ingredients WHERE pastryId = :pastryId")
    suspend fun deleteIngredientsForPastry(pastryId: Int)

    @Insert
    suspend fun insertSteps(steps: List<Step>)

    @Query("DELETE FROM steps WHERE pastryId = :pastryId")
    suspend fun deleteStepsForPastry(pastryId: Int)

    @Insert
    suspend fun insertMedia(media: List<MediaItem>)

    @Query("DELETE FROM media WHERE pastryId = :pastryId")
    suspend fun deleteMediaForPastry(pastryId: Int)

    @Delete
    suspend fun deletePastry(pastry: Pastry)

    @Query("UPDATE pastries SET isFavorite = :favorite WHERE id = :pastryId")
    suspend fun setFavorite(pastryId: Int, favorite: Boolean)

    @Transaction
    @Query("SELECT * FROM pastries ORDER BY id DESC")
    fun getAllPastriesWithIngredients(): Flow<List<PastryWithIngredients>>

    @Transaction
    @Query("SELECT * FROM pastries WHERE id = :id")
    fun getPastryWithIngredients(id: Int): Flow<PastryWithIngredients?>

    @Transaction
    @Query("SELECT * FROM pastries WHERE id = :id")
    suspend fun getPastryWithIngredientsOnce(id: Int): PastryWithIngredients?

    // --- Tags ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findTagByName(name: String): Tag?

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPastryTagCrossRef(crossRef: PastryTagCrossRef)

    @Query("DELETE FROM pastry_tag WHERE pastryId = :pastryId")
    suspend fun clearTagsForPastry(pastryId: Int)
}
