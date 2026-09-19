package com.lovely.bakingrecipes.util

import com.lovely.bakingrecipes.data.Ingredient
import com.lovely.bakingrecipes.data.IngredientUnit
import com.lovely.bakingrecipes.data.Pastry
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.Step
import org.json.JSONArray
import org.json.JSONObject

// Serializes/deserializes the whole recipe library to a portable JSON document.
object BackupManager {

    private const val VERSION = 1

    data class ImportedRecipe(
        val pastry: Pastry,
        val ingredients: List<Ingredient>,
        val steps: List<Step>,
        val tags: List<String>
    )

    fun toJson(recipes: List<PastryWithIngredients>): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val array = JSONArray()
        recipes.forEach { recipe ->
            val p = recipe.pastry
            val obj = JSONObject()
            obj.put("name", p.name)
            obj.put("category", p.category)
            obj.put("description", p.description)
            obj.put("servings", p.servings)
            obj.put("prepMinutes", p.prepMinutes)
            obj.put("cookMinutes", p.cookMinutes)
            obj.put("difficulty", p.difficulty)
            obj.put("isFavorite", p.isFavorite)

            val ingredients = JSONArray()
            recipe.ingredients.forEach { ing ->
                ingredients.put(
                    JSONObject().apply {
                        put("name", ing.name)
                        put("amount", ing.amount)
                        put("unit", ing.unit.name)
                    }
                )
            }
            obj.put("ingredients", ingredients)

            val steps = JSONArray()
            recipe.steps.sortedBy { it.position }.forEach { step ->
                steps.put(step.instruction)
            }
            obj.put("steps", steps)

            val tags = JSONArray()
            recipe.tags.forEach { tags.put(it.name) }
            obj.put("tags", tags)

            array.put(obj)
        }
        root.put("recipes", array)
        return root.toString(2)
    }

    fun fromJson(json: String): List<ImportedRecipe> {
        val root = JSONObject(json)
        val array = root.optJSONArray("recipes") ?: return emptyList()
        val result = mutableListOf<ImportedRecipe>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val pastry = Pastry(
                name = obj.optString("name"),
                category = obj.optString("category"),
                description = obj.optString("description"),
                imageUri = null,
                servings = obj.optInt("servings"),
                prepMinutes = obj.optInt("prepMinutes"),
                cookMinutes = obj.optInt("cookMinutes"),
                difficulty = obj.optString("difficulty"),
                isFavorite = obj.optBoolean("isFavorite")
            )

            val ingredients = mutableListOf<Ingredient>()
            obj.optJSONArray("ingredients")?.let { arr ->
                for (j in 0 until arr.length()) {
                    val ing = arr.getJSONObject(j)
                    ingredients.add(
                        Ingredient(
                            pastryId = 0,
                            name = ing.optString("name"),
                            amount = ing.optDouble("amount", 0.0),
                            unit = runCatching {
                                IngredientUnit.valueOf(ing.optString("unit"))
                            }.getOrDefault(IngredientUnit.GRAMS)
                        )
                    )
                }
            }

            val steps = mutableListOf<Step>()
            obj.optJSONArray("steps")?.let { arr ->
                for (j in 0 until arr.length()) {
                    steps.add(Step(pastryId = 0, position = j, instruction = arr.optString(j)))
                }
            }

            val tags = mutableListOf<String>()
            obj.optJSONArray("tags")?.let { arr ->
                for (j in 0 until arr.length()) tags.add(arr.optString(j))
            }

            result.add(ImportedRecipe(pastry, ingredients, steps, tags))
        }
        return result
    }
}
