package com.lovely.bakingrecipes.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaType {
    PHOTO,
    VIDEO
}

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = Pastry::class,
            parentColumns = ["id"],
            childColumns = ["pastryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pastryId")]
)
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pastryId: Int,
    val uri: String,
    val type: MediaType,
    val position: Int
)
