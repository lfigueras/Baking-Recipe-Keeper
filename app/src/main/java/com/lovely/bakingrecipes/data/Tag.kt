package com.lovely.bakingrecipes.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)

@Entity(
    tableName = "pastry_tag",
    primaryKeys = ["pastryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Pastry::class,
            parentColumns = ["id"],
            childColumns = ["pastryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pastryId"), Index("tagId")]
)
data class PastryTagCrossRef(
    val pastryId: Int,
    val tagId: Int
)
