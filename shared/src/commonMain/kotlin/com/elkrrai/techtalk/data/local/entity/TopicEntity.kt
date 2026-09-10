package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = TechnologyEntity::class,
            parentColumns = ["id"],
            childColumns = ["technologyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("technologyId")]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val technologyId: Long,
    val name: String,
    val description: String = ""
)
