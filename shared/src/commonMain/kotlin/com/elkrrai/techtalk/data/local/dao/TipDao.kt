package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.elkrrai.techtalk.data.local.entity.TipEntity
import com.elkrrai.techtalk.data.local.entity.TipWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TipDao {
    @Upsert
    suspend fun upsert(tip: TipEntity): Long

    @Upsert
    suspend fun upsertAll(tips: List<TipEntity>)

    /**
     * Canonical feed query — reproduce verbatim. Feed only contains tips from
     * subscribed technologies; seen tips are excluded; ordering is deterministic
     * (`ORDER BY tips.id`) — shuffling happens once in the UI layer, never in SQL.
     */
    @Query(
        """
        SELECT tips.* FROM tips
        INNER JOIN topics ON tips.topicId = topics.id
        INNER JOIN user_subscriptions ON topics.technologyId = user_subscriptions.technologyId
        WHERE tips.id NOT IN (
            SELECT tipId FROM user_tip_history WHERE isSeen = 1
        )
        ORDER BY tips.id
        """
    )
    fun observeFeedTips(): Flow<List<TipEntity>>

    @Query(
        """
        SELECT tips.*, topics.name AS topicName, technologies.name AS technologyName,
               technologies.tagColor AS technologyTagColor
        FROM tips
        INNER JOIN topics ON tips.topicId = topics.id
        INNER JOIN technologies ON topics.technologyId = technologies.id
        INNER JOIN user_subscriptions ON topics.technologyId = user_subscriptions.technologyId
        WHERE tips.id NOT IN (
            SELECT tipId FROM user_tip_history WHERE isSeen = 1
        )
        ORDER BY tips.id
        """
    )
    fun observeFeedTipsWithDetails(): Flow<List<TipWithDetails>>

    /** Unfiltered snapshot (no `isSeen` filter) — used for browsing all subscribed content. */
    @Query(
        """
        SELECT tips.*, topics.name AS topicName, technologies.name AS technologyName,
               technologies.tagColor AS technologyTagColor
        FROM tips
        INNER JOIN topics ON tips.topicId = topics.id
        INNER JOIN technologies ON topics.technologyId = technologies.id
        INNER JOIN user_subscriptions ON topics.technologyId = user_subscriptions.technologyId
        ORDER BY tips.id
        """
    )
    suspend fun getFeedTipsWithDetails(): List<TipWithDetails>

    @Query("SELECT * FROM tips ORDER BY tips.id LIMIT :limit")
    suspend fun getFeedTips(limit: Int = 20): List<TipEntity>

    @Query("SELECT * FROM tips WHERE topicId = :topicId")
    suspend fun getByTopicId(topicId: Long): List<TipEntity>

    @Query("SELECT * FROM tips WHERE id = :id")
    suspend fun getById(id: Long): TipEntity?
}
