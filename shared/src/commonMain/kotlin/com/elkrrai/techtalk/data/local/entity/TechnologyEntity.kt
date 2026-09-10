package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [name] is matched **by name** during pack import (`COLLATE NOCASE`). */
@Entity(tableName = "technologies")
data class TechnologyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconResource: String? = null,
    val description: String = "",
    val tagColor: String = "#9E9E9E"
)
