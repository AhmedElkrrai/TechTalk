package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** No FK — this is a denormalized snapshot (`technologyName` copied) that must survive
 * content deletion. `status` holds the name of [com.elkrrai.techtalk.domain.model
 * .battle.BattleStatus]. */
@Entity(tableName = "battle_history")
data class BattleHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val technologyId: Long,
    val technologyName: String,
    val score: Int,
    val totalQuestions: Int,
    val status: String,
    val xpGained: Int,
    val playedAt: Long
)
