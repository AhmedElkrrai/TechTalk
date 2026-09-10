package com.elkrrai.techtalk.data.repository.mapper

import com.elkrrai.techtalk.data.local.entity.AnswerEntity
import com.elkrrai.techtalk.data.local.entity.BattleHistoryEntity
import com.elkrrai.techtalk.data.local.entity.QuestionEntity
import com.elkrrai.techtalk.data.local.entity.TechnologyEntity
import com.elkrrai.techtalk.data.local.entity.TipEntity
import com.elkrrai.techtalk.data.local.entity.TipWithDetails
import com.elkrrai.techtalk.data.local.entity.TopicEntity
import com.elkrrai.techtalk.data.local.entity.UserProfileEntity
import com.elkrrai.techtalk.domain.model.battle.BattleAnswer
import com.elkrrai.techtalk.domain.model.battle.BattleHistoryItem
import com.elkrrai.techtalk.domain.model.battle.BattleQuestion
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.tech.TechnologyInfo
import com.elkrrai.techtalk.domain.model.tip.FeedTip
import com.elkrrai.techtalk.domain.model.tip.TipInfo
import com.elkrrai.techtalk.domain.model.tip.TopicInfo
import com.elkrrai.techtalk.domain.model.user.UserProfile

// Entities never cross the repository boundary — these mappers are the only place
// that translates between the two.

internal fun TechnologyEntity.toDomain(): TechnologyInfo = TechnologyInfo(
    id = id,
    name = name,
    iconResourceName = iconResource,
    description = description,
    tagColor = tagColor
)

internal fun TopicEntity.toDomain(): TopicInfo = TopicInfo(
    id = id,
    technologyId = technologyId,
    name = name,
    description = description
)

internal fun TipEntity.toDomain(): TipInfo = TipInfo(
    id = id,
    topicId = topicId,
    title = title,
    content = content,
    codeSnippet = codeSnippet,
    codeLang = codeLang,
    difficulty = difficulty,
    interestCount = interestCount,
    createdAt = createdAt
)

internal fun TipWithDetails.toDomain(): FeedTip = FeedTip(
    id = tip.id,
    topicId = tip.topicId,
    title = tip.title,
    content = tip.content,
    codeSnippet = tip.codeSnippet,
    codeLang = tip.codeLang,
    difficulty = tip.difficulty,
    interestCount = tip.interestCount,
    createdAt = tip.createdAt,
    topicName = topicName,
    technologyName = technologyName,
    technologyTagColor = technologyTagColor
)

internal fun QuestionEntity.toDomain(): BattleQuestion = BattleQuestion(
    id = id,
    topicId = topicId,
    questionText = questionText,
    difficulty = difficulty,
    explanation = explanation
)

internal fun AnswerEntity.toDomain(): BattleAnswer = BattleAnswer(
    id = id,
    questionId = questionId,
    answerText = answerText,
    isCorrect = isCorrect
)

internal fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    name = name,
    avatarKey = avatarKey,
    level = level,
    currentXp = currentXp
)

internal fun BattleHistoryEntity.toDomain(): BattleHistoryItem = BattleHistoryItem(
    id = id,
    technologyName = technologyName,
    score = score,
    totalQuestions = totalQuestions,
    status = runCatching { BattleStatus.valueOf(status) }.getOrDefault(BattleStatus.LOSS),
    xpGained = xpGained,
    playedAt = playedAt
)
