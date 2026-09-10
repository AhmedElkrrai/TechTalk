package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elkrrai.techtalk.domain.model.common.Difficulty

/** Attached to a **topic**, not directly to a technology; technology is reached via
 * `questions -> topics -> technologies`. */
@Entity(
    tableName = "questions",
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
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val questionText: String,
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val explanation: String = ""
)
