package com.lovely.bakingrecipes.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromUnit(unit: IngredientUnit): String = unit.name

    @TypeConverter
    fun toUnit(value: String): IngredientUnit = IngredientUnit.valueOf(value)

    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)
}
