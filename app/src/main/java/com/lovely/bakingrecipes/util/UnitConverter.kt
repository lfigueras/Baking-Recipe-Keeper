package com.lovely.bakingrecipes.util

import com.lovely.bakingrecipes.data.IngredientUnit

// Converts ingredient amounts between compatible units and picks readable units when scaling.
object UnitConverter {

    enum class Dimension { MASS, VOLUME, COUNT }

    enum class System { ORIGINAL, METRIC, US }

    // Factor to convert one unit into its dimension's base (grams for mass, millilitres for volume).
    private val toBase: Map<IngredientUnit, Double> = mapOf(
        IngredientUnit.GRAMS to 1.0,
        IngredientUnit.KILOGRAMS to 1000.0,
        IngredientUnit.OUNCES to 28.349523125,
        IngredientUnit.POUNDS to 453.59237,
        IngredientUnit.MILLILITERS to 1.0,
        IngredientUnit.LITERS to 1000.0,
        IngredientUnit.TEASPOONS to 4.92892159375,
        IngredientUnit.TABLESPOONS to 14.78676478125,
        IngredientUnit.CUPS to 236.5882365
    )

    fun dimensionOf(unit: IngredientUnit): Dimension = when (unit) {
        IngredientUnit.GRAMS, IngredientUnit.KILOGRAMS,
        IngredientUnit.OUNCES, IngredientUnit.POUNDS -> Dimension.MASS

        IngredientUnit.MILLILITERS, IngredientUnit.LITERS,
        IngredientUnit.TEASPOONS, IngredientUnit.TABLESPOONS, IngredientUnit.CUPS -> Dimension.VOLUME

        IngredientUnit.PIECES, IngredientUnit.PINCH -> Dimension.COUNT
    }

    fun canConvert(from: IngredientUnit, to: IngredientUnit): Boolean =
        toBase.containsKey(from) && toBase.containsKey(to) &&
            dimensionOf(from) == dimensionOf(to)

    fun convert(amount: Double, from: IngredientUnit, to: IngredientUnit): Double? {
        if (!canConvert(from, to)) return null
        val base = amount * (toBase[from] ?: return null)
        return base / (toBase[to] ?: return null)
    }

    // Returns a scaled amount rendered in the requested measurement system, choosing a readable unit.
    fun display(amount: Double, unit: IngredientUnit, system: System): Pair<Double, IngredientUnit> {
        val dimension = dimensionOf(unit)
        if (dimension == Dimension.COUNT || system == System.ORIGINAL) {
            return amount to unit
        }
        val baseAmount = amount * (toBase[unit] ?: return amount to unit)
        val ladder = ladderFor(dimension, system)
        return normalize(baseAmount, ladder)
    }

    // Picks the largest unit whose amount stays >= 1, keeping numbers friendly (e.g. 1500 g -> 1.5 kg).
    private fun normalize(baseAmount: Double, ladder: List<IngredientUnit>): Pair<Double, IngredientUnit> {
        for (candidate in ladder) {
            val factor = toBase[candidate] ?: continue
            val value = baseAmount / factor
            if (value >= 1.0) return value to candidate
        }
        val smallest = ladder.last()
        return (baseAmount / (toBase[smallest] ?: 1.0)) to smallest
    }

    // Units ordered largest first for readable normalization.
    private fun ladderFor(dimension: Dimension, system: System): List<IngredientUnit> = when (dimension) {
        Dimension.MASS -> when (system) {
            System.US -> listOf(IngredientUnit.POUNDS, IngredientUnit.OUNCES)
            else -> listOf(IngredientUnit.KILOGRAMS, IngredientUnit.GRAMS)
        }
        Dimension.VOLUME -> when (system) {
            System.US -> listOf(
                IngredientUnit.CUPS,
                IngredientUnit.TABLESPOONS,
                IngredientUnit.TEASPOONS
            )
            else -> listOf(IngredientUnit.LITERS, IngredientUnit.MILLILITERS)
        }
        Dimension.COUNT -> emptyList()
    }
}
