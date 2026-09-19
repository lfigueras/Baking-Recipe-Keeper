package com.lovely.bakingrecipes.repository

import com.lovely.bakingrecipes.data.Ingredient
import com.lovely.bakingrecipes.data.MediaItem
import com.lovely.bakingrecipes.data.Pastry
import com.lovely.bakingrecipes.data.PastryDao
import com.lovely.bakingrecipes.data.PastryTagCrossRef
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.Step
import com.lovely.bakingrecipes.data.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PastryRepository(
    private val pastryDao: PastryDao
) {

    val allPastries: Flow<List<PastryWithIngredients>> =
        pastryDao.getAllPastriesWithIngredients()

    val allTags: Flow<List<Tag>> = pastryDao.getAllTags()

    fun getPastryById(id: Int): Flow<PastryWithIngredients?> =
        pastryDao.getPastryWithIngredients(id)

    suspend fun insertPastryWithIngredients(
        pastry: Pastry,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        tags: List<String> = emptyList(),
        media: List<MediaItem> = emptyList()
    ) {
        val pastryId = pastryDao.insertPastry(pastry).toInt()
        if (ingredients.isNotEmpty()) {
            pastryDao.insertIngredients(ingredients.map { it.copy(pastryId = pastryId) })
        }
        if (steps.isNotEmpty()) {
            pastryDao.insertSteps(
                steps.mapIndexed { index, step -> step.copy(pastryId = pastryId, position = index) }
            )
        }
        if (media.isNotEmpty()) {
            pastryDao.insertMedia(
                media.mapIndexed { index, m -> m.copy(id = 0, pastryId = pastryId, position = index) }
            )
        }
        applyTags(pastryId, tags)
    }

    suspend fun updatePastryWithIngredients(
        pastry: Pastry,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        tags: List<String> = emptyList(),
        media: List<MediaItem> = emptyList()
    ) {
        pastryDao.updatePastry(pastry)
        pastryDao.deleteIngredientsForPastry(pastry.id)
        if (ingredients.isNotEmpty()) {
            pastryDao.insertIngredients(ingredients.map { it.copy(pastryId = pastry.id) })
        }
        pastryDao.deleteStepsForPastry(pastry.id)
        if (steps.isNotEmpty()) {
            pastryDao.insertSteps(
                steps.mapIndexed { index, step -> step.copy(pastryId = pastry.id, position = index) }
            )
        }
        pastryDao.deleteMediaForPastry(pastry.id)
        if (media.isNotEmpty()) {
            pastryDao.insertMedia(
                media.mapIndexed { index, m -> m.copy(id = 0, pastryId = pastry.id, position = index) }
            )
        }
        pastryDao.clearTagsForPastry(pastry.id)
        applyTags(pastry.id, tags)
    }

    suspend fun deletePastry(pastry: Pastry) {
        pastryDao.deletePastry(pastry)
    }

    suspend fun getAllOnce(): List<PastryWithIngredients> =
        pastryDao.getAllPastriesWithIngredients().first()

    // Inserts imported recipes as new entries (does not overwrite existing ones).
    suspend fun importRecipes(
        recipes: List<Triple<Pastry, Pair<List<Ingredient>, List<Step>>, List<String>>>
    ) {
        recipes.forEach { (pastry, content, tags) ->
            insertPastryWithIngredients(pastry, content.first, content.second, tags)
        }
    }

    suspend fun setFavorite(pastryId: Int, favorite: Boolean) {
        pastryDao.setFavorite(pastryId, favorite)
    }

    // Deep-copies a recipe (metadata, ingredients, steps, tags) as a new "(Copy)".
    suspend fun duplicatePastry(source: PastryWithIngredients): Int {
        val now = System.currentTimeMillis()
        val newPastry = source.pastry.copy(
            id = 0,
            name = "${source.pastry.name} (Copy)",
            isFavorite = false,
            createdAt = now,
            updatedAt = now
        )
        val newId = pastryDao.insertPastry(newPastry).toInt()
        if (source.ingredients.isNotEmpty()) {
            pastryDao.insertIngredients(
                source.ingredients.map { it.copy(id = 0, pastryId = newId) }
            )
        }
        if (source.steps.isNotEmpty()) {
            pastryDao.insertSteps(
                source.steps.sortedBy { it.position }
                    .mapIndexed { index, step -> step.copy(id = 0, pastryId = newId, position = index) }
            )
        }
        if (source.media.isNotEmpty()) {
            pastryDao.insertMedia(
                source.media.sortedBy { it.position }
                    .mapIndexed { index, m -> m.copy(id = 0, pastryId = newId, position = index) }
            )
        }
        applyTags(newId, source.tags.map { it.name })
        return newId
    }

    private suspend fun applyTags(pastryId: Int, tags: List<String>) {
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .forEach { name ->
                // insertTag ignores duplicates (unique name); fall back to a lookup.
                val insertedId = pastryDao.insertTag(Tag(name = name)).toInt()
                val tagId = if (insertedId > 0) insertedId else pastryDao.findTagByName(name)?.id
                if (tagId != null) {
                    pastryDao.insertPastryTagCrossRef(PastryTagCrossRef(pastryId, tagId))
                }
            }
    }
}