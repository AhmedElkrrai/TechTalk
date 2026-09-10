package com.elkrrai.techtalk.domain.repository

import com.elkrrai.techtalk.domain.model.battle.BattleAnswer
import com.elkrrai.techtalk.domain.model.battle.BattleHistoryItem
import com.elkrrai.techtalk.domain.model.battle.BattleHistorySummary
import com.elkrrai.techtalk.domain.model.battle.BattleQuestion
import com.elkrrai.techtalk.domain.model.battle.BattlePack
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.content.ImportResult
import com.elkrrai.techtalk.domain.model.tech.TechnologyInfo
import com.elkrrai.techtalk.domain.model.tip.FeedTip
import com.elkrrai.techtalk.domain.model.tip.TipInfo
import com.elkrrai.techtalk.domain.model.tip.TipPack
import com.elkrrai.techtalk.domain.model.tip.TopicInfo
import com.elkrrai.techtalk.domain.model.user.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * The local/offline contract. `observeX()` returns cold [Flow]s and is not suspend;
 * `getX()` is a one-shot suspend function. ID sets are returned as [Set] to make
 * membership checks cheap in the UI. Implemented by `data.repository
 * .TechTalkRepositoryImpl`; the UI only ever sees this interface.
 */
interface TechTalkRepository {

    // Lifecycle
    suspend fun initialize()

    // Catalog
    fun observeAllTechnologies(): Flow<List<TechnologyInfo>>
    suspend fun getAllTechnologies(): List<TechnologyInfo>
    suspend fun getTechnologyById(id: Long): TechnologyInfo?
    fun observeTopics(technologyId: Long): Flow<List<TopicInfo>>
    suspend fun getTopics(technologyId: Long): List<TopicInfo>

    // Feed
    fun observeFeedTips(): Flow<List<TipInfo>>
    fun observeFeedTipsWithDetails(): Flow<List<FeedTip>>
    suspend fun getFeedTipsWithDetails(): List<FeedTip>
    suspend fun getFeedTips(limit: Int = 20): List<TipInfo>

    // Subscriptions
    suspend fun subscribe(technologyId: Long)
    suspend fun unsubscribe(technologyId: Long)
    suspend fun isSubscribed(technologyId: Long): Boolean
    fun observeIsSubscribed(technologyId: Long): Flow<Boolean>
    fun observeSubscribedTechnologies(): Flow<List<TechnologyInfo>>

    // Profile
    fun observeUserProfile(): Flow<UserProfile>
    suspend fun updateUserName(name: String)
    suspend fun updateUserAvatar(avatarKey: String)
    suspend fun awardBattleXp(xpGained: Int): UserProfile

    // Battle history
    fun observeBattleHistorySummary(): Flow<BattleHistorySummary>
    fun observeRecentBattles(limit: Int = 10): Flow<List<BattleHistoryItem>>
    suspend fun recordBattleResult(
        technologyId: Long,
        technologyName: String,
        score: Int,
        totalQuestions: Int,
        status: BattleStatus,
        xpGained: Int
    )

    // Tip history
    suspend fun markTipSeen(tipId: Long)
    suspend fun toggleInterested(tipId: Long)
    suspend fun isTipInterested(tipId: Long): Boolean
    suspend fun getInterestedTipIds(): Set<Long>
    suspend fun getSeenTipIds(): Set<Long>
    suspend fun getSeenCount(): Int
    suspend fun getInterestedCount(): Int
    suspend fun resetHistory()

    // Battle content
    suspend fun getQuestionIdsByTechnology(technologyId: Long): List<Long>
    suspend fun getQuestionIdsByTechnologyAndDifficulty(
        technologyId: Long,
        difficulty: Difficulty
    ): List<Long>
    suspend fun getQuestionById(id: Long): BattleQuestion?
    suspend fun getAnswersByQuestionId(questionId: Long): List<BattleAnswer>

    // Content IO
    suspend fun importTipPack(json: String): ImportResult
    suspend fun importBattlePack(json: String): Int
    suspend fun exportTipPack(tipIds: List<Long>, author: String = "", description: String = ""): String
    fun serializeTipPack(pack: TipPack): String
    suspend fun saveTipPackToFile(fileName: String, jsonContent: String): String
    suspend fun loadTipPackFromFile(filePath: String): String
    suspend fun listTipPackFiles(): List<String>
    suspend fun deleteTipPackFile(fileName: String): Boolean
    suspend fun exportAllTipsByTopic(): Int
    suspend fun exportAllBattlesByTechs(): Int
}
