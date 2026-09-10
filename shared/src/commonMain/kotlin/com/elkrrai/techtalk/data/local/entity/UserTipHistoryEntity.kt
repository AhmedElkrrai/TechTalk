package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.elkrrai.techtalk.data.utils.currentEpochMillis

/** `isSeen` is set via INSERT-IGNORE (`markSeen`), so [seenAt] reflects the *first*
 * view only. `toggleInterested` updates an existing row only — a tip must be marked
 * seen before it can be toggled interesting. */
@Entity(
    tableName = "user_tip_history",
    foreignKeys = [
        ForeignKey(
            entity = TipEntity::class,
            parentColumns = ["id"],
            childColumns = ["tipId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserTipHistoryEntity(
    @PrimaryKey val tipId: Long,
    val isSeen: Boolean = false,
    val isInterested: Boolean = false,
    val seenAt: Long = currentEpochMillis()
)
