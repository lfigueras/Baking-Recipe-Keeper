package com.lovely.bakingrecipes.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PastryWithIngredients(
    @Embedded val pastry: Pastry,
    @Relation(
        parentColumn = "id",
        entityColumn = "pastryId"
    )
    val ingredients: List<Ingredient>,
    @Relation(
        parentColumn = "id",
        entityColumn = "pastryId"
    )
    val steps: List<Step> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "pastryId"
    )
    val media: List<MediaItem> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PastryTagCrossRef::class,
            parentColumn = "pastryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag> = emptyList()
)
