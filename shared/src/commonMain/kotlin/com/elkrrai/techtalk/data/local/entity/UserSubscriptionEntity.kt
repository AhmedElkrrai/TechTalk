package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.elkrrai.techtalk.data.utils.currentEpochMillis

@Entity(
    tableName = "user_subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = TechnologyEntity::class,
            parentColumns = ["id"],
            childColumns = ["technologyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSubscriptionEntity(
    @PrimaryKey val technologyId: Long,
    val subscribedAt: Long = currentEpochMillis()
)
