package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elkrrai.techtalk.data.utils.currentEpochMillis
import com.elkrrai.techtalk.domain.model.common.Difficulty

@Entity(
    tableName = "tips",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId")]
)
data class TipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val title: String,
    val content: String,
    val codeSnippet: String? = null,
    val codeLang: String? = null,
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val interestCount: Int = 0,
    val createdAt: Long = currentEpochMillis()
)
