package com.elkrrai.techtalk.data.repository

import com.elkrrai.techtalk.data.local.content.battle.BattlePackManager
import com.elkrrai.techtalk.data.local.content.tip.TipPackFileHandler
import com.elkrrai.techtalk.data.local.content.tip.TipPackManager
import com.elkrrai.techtalk.data.local.dao.BattleHistoryDao
import com.elkrrai.techtalk.data.local.dao.QuestionDao
import com.elkrrai.techtalk.data.local.dao.TechnologyDao
import com.elkrrai.techtalk.data.local.dao.TipDao
import com.elkrrai.techtalk.data.local.dao.TopicDao
import com.elkrrai.techtalk.data.local.dao.UserProfileDao
import com.elkrrai.techtalk.data.local.dao.UserSubscriptionDao
import com.elkrrai.techtalk.data.local.dao.UserTipHistoryDao
import com.elkrrai.techtalk.data.local.entity.BattleHistoryEntity
import com.elkrrai.techtalk.data.local.entity.UserProfileEntity
import com.elkrrai.techtalk.data.local.entity.UserSubscriptionEntity
import com.elkrrai.techtalk.data.local.entity.UserTipHistoryEntity
import com.elkrrai.techtalk.data.local.seed.DatabaseSeeder
import com.elkrrai.techtalk.data.repository.mapper.toDomain
import com.elkrrai.techtalk.data.utils.currentEpochMillis
import com.elkrrai.techtalk.domain.model.battle.BattleAnswer
import com.elkrrai.techtalk.domain.model.battle.BattleHistoryItem
import com.elkrrai.techtalk.domain.model.battle.BattleHistorySummary
import com.elkrrai.techtalk.domain.model.battle.BattleProgression
import com.elkrrai.techtalk.domain.model.battle.BattleQuestion
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.content.ImportResult
import com.elkrrai.techtalk.domain.model.tech.TechnologyInfo
import com.elkrrai.techtalk.domain.model.tip.FeedTip
import com.elkrrai.techtalk.domain.model.tip.TipInfo
import com.elkrrai.techtalk.domain.model.tip.TipPack
import com.elkrrai.techtalk.domain.model.tip.TopicInfo
import com.elkrrai.techtalk.domain.model.user.UserProfile
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TechTalkRepositoryImpl(
    private val technologyDao: TechnologyDao,
    private val topicDao: TopicDao,
    private val tipDao: TipDao,
    private val questionDao: QuestionDao,
    private val userProfileDao: UserProfileDao,
    private val userSubscriptionDao: UserSubscriptionDao,
    private val userTipHistoryDao: UserTipHistoryDao,
    private val battleHistoryDao: BattleHistoryDao,
    private val seeder: DatabaseSeeder,
    private val tipPackManager: TipPackManager,
    private val battlePackManager: BattlePackManager,
    private val fileHandler: TipPackFileHandler
) : TechTalkRepository {

    // Lifecycle

    override suspend fun initialize() {
        seeder.seedIfEmpty()
    }

    // Catalog

    override fun observeAllTechnologies(): Flow<List<TechnologyInfo>> =
        technologyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllTechnologies(): List<TechnologyInfo> =
        technologyDao.getAll().map { it.toDomain() }

    override suspend fun getTechnologyById(id: Long): TechnologyInfo? =
        technologyDao.getById(id)?.toDomain()

    override fun observeTopics(technologyId: Long): Flow<List<TopicInfo>> =
        topicDao.observeByTechnology(technologyId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTopics(technologyId: Long): List<TopicInfo> =
        topicDao.getByTechnologyId(technologyId).map { it.toDomain() }

    // Feed

    override fun observeFeedTips(): Flow<List<TipInfo>> =
        tipDao.observeFeedTips().map { list -> list.map { it.toDomain() } }

    override fun observeFeedTipsWithDetails(): Flow<List<FeedTip>> =
        tipDao.observeFeedTipsWithDetails().map { list -> list.map { it.toDomain() } }

    override suspend fun getFeedTipsWithDetails(): List<FeedTip> =
        tipDao.getFeedTipsWithDetails().map { it.toDomain() }

    override suspend fun getFeedTips(limit: Int): List<TipInfo> =
        tipDao.getFeedTips(limit).map { it.toDomain() }

    // Subscriptions

    override suspend fun subscribe(technologyId: Long) {
        userSubscriptionDao.subscribe(UserSubscriptionEntity(technologyId = technologyId))
    }

    override suspend fun unsubscribe(technologyId: Long) {
        userSubscriptionDao.unsubscribe(technologyId)
    }

    override suspend fun isSubscribed(technologyId: Long): Boolean =
        userSubscriptionDao.isSubscribed(technologyId)

    override fun observeIsSubscribed(technologyId: Long): Flow<Boolean> =
        userSubscriptionDao.observeIsSubscribed(technologyId)

    override fun observeSubscribedTechnologies(): Flow<List<TechnologyInfo>> =
        userSubscriptionDao.observeSubscribedTechnologies().map { list -> list.map { it.toDomain() } }

    // Profile

    override fun observeUserProfile(): Flow<UserProfile> =
        userProfileDao.observe().map { (it ?: UserProfileEntity()).toDomain() }

    override suspend fun updateUserName(name: String) {
        val current = userProfileDao.get() ?: UserProfileEntity()
        userProfileDao.upsert(current.copy(name = name.trim(), updatedAt = currentEpochMillis()))
    }

    override suspend fun updateUserAvatar(avatarKey: String) {
        val current = userProfileDao.get() ?: UserProfileEntity()
        userProfileDao.upsert(current.copy(avatarKey = avatarKey, updatedAt = currentEpochMillis()))
    }

    override suspend fun awardBattleXp(xpGained: Int): UserProfile {
        val current = userProfileDao.get() ?: UserProfileEntity()
        val (newLevel, newXp) = BattleProgression.applyXp(current.level, current.currentXp, xpGained)
        val updated = current.copy(level = newLevel, currentXp = newXp, updatedAt = currentEpochMillis())
        userProfileDao.upsert(updated)
        return updated.toDomain()
    }

    // Battle history

    override fun observeBattleHistorySummary(): Flow<BattleHistorySummary> =
        battleHistoryDao.observeAll().map { computeBattleHistorySummary(it) }

    override fun observeRecentBattles(limit: Int): Flow<List<BattleHistoryItem>> =
        battleHistoryDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun recordBattleResult(
        technologyId: Long,
        technologyName: String,
        score: Int,
        totalQuestions: Int,
        status: BattleStatus,
        xpGained: Int
    ) {
        battleHistoryDao.insert(
            BattleHistoryEntity(
                technologyId = technologyId,
                technologyName = technologyName,
                score = score,
                totalQuestions = totalQuestions,
                status = status.name,
                xpGained = xpGained,
                playedAt = currentEpochMillis()
            )
        )
    }

    /** [entities] arrive DESC-ordered (`playedAt DESC, id DESC`) — the current streak
     * is the leading run of wins; the best streak is the longest run anywhere. Status
     * strings are parsed defensively; an unknown value degrades to "not a win". */
    private fun computeBattleHistorySummary(entities: List<BattleHistoryEntity>): BattleHistorySummary {
        if (entities.isEmpty()) return BattleHistorySummary()

        val statuses = entities.map { runCatching { BattleStatus.valueOf(it.status) }.getOrDefault(BattleStatus.LOSS) }
        val battlesPlayed = entities.size
        val wins = statuses.count { it == BattleStatus.WIN }
        val losses = battlesPlayed - wins
        val winRate = if (battlesPlayed == 0) 0 else (wins * 100) / battlesPlayed
        val highestScore = entities.maxOf { it.score }
        val currentWinStreak = statuses.takeWhile { it == BattleStatus.WIN }.size

        var bestStreak = 0
        var running = 0
        for (status in statuses) {
            if (status == BattleStatus.WIN) {
                running++
                bestStreak = maxOf(bestStreak, running)
            } else {
                running = 0
            }
        }

        return BattleHistorySummary(
            battlesPlayed = battlesPlayed,
            wins = wins,
            losses = losses,
            winRate = winRate,
            highestScore = highestScore,
            currentWinStreak = currentWinStreak,
            bestWinStreak = bestStreak
        )
    }

    // Tip history

    override suspend fun markTipSeen(tipId: Long) {
        userTipHistoryDao.markSeen(UserTipHistoryEntity(tipId = tipId, isSeen = true, seenAt = currentEpochMillis()))
    }

    override suspend fun toggleInterested(tipId: Long) {
        userTipHistoryDao.toggleInterested(tipId)
    }

    override suspend fun isTipInterested(tipId: Long): Boolean =
        userTipHistoryDao.isInterested(tipId) ?: false

    override suspend fun getInterestedTipIds(): Set<Long> =
        userTipHistoryDao.getInterestedTipIds().toSet()

    override suspend fun getSeenTipIds(): Set<Long> =
        userTipHistoryDao.getSeenTipIds().toSet()

    override suspend fun getSeenCount(): Int = userTipHistoryDao.getSeenCount()

    override suspend fun getInterestedCount(): Int = userTipHistoryDao.getInterestedCount()

    override suspend fun resetHistory() {
        userTipHistoryDao.clearAll()
    }

    // Battle content

    override suspend fun getQuestionIdsByTechnology(technologyId: Long): List<Long> =
        questionDao.getQuestionIdsByTechnology(technologyId)

    override suspend fun getQuestionIdsByTechnologyAndDifficulty(
        technologyId: Long,
        difficulty: Difficulty
    ): List<Long> = questionDao.getQuestionIdsByTechnologyAndDifficulty(technologyId, difficulty)

    override suspend fun getQuestionById(id: Long): BattleQuestion? =
        questionDao.getQuestionById(id)?.toDomain()

    override suspend fun getAnswersByQuestionId(questionId: Long): List<BattleAnswer> =
        questionDao.getAnswersByQuestionId(questionId).map { it.toDomain() }

    // Content IO

    override suspend fun importTipPack(json: String): ImportResult =
        tipPackManager.importFromJson(json)

    override suspend fun importBattlePack(json: String): Int =
        battlePackManager.importFromJson(json)

    override suspend fun exportTipPack(tipIds: List<Long>, author: String, description: String): String =
        tipPackManager.exportFromDatabase(tipIds, author, description)

    override fun serializeTipPack(pack: TipPack): String = tipPackManager.export(pack)

    override suspend fun saveTipPackToFile(fileName: String, jsonContent: String): String =
        fileHandler.saveToFile(fileName, jsonContent)

    override suspend fun loadTipPackFromFile(filePath: String): String =
        fileHandler.readFromFile(filePath)

    override suspend fun listTipPackFiles(): List<String> = fileHandler.listFiles()

    override suspend fun deleteTipPackFile(fileName: String): Boolean = fileHandler.deleteFile(fileName)

    override suspend fun exportAllTipsByTopic(): Int {
        val files = tipPackManager.exportAllByTopic()
        files.forEach { (fileName, json) -> fileHandler.saveToFile(fileName, json) }
        return files.size
    }

    override suspend fun exportAllBattlesByTechs(): Int {
        val files = battlePackManager.exportAllByTechs()
        files.forEach { (fileName, json) -> fileHandler.saveToFile(fileName, json) }
        return files.size
    }
}
