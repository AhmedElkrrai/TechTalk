package com.elkrrai.techtalk.domain.model.battle

import kotlinx.serialization.Serializable

/**
 * Portable battle-question content format. References technologies/topics **by name**.
 * Convention: 4 answers per question, exactly one `isCorrect = true`; questions with no
 * correct answer are dropped at import.
 */
@Serializable
data class BattlePack(
    val version: Int = 1,
    val author: String = "",
    val description: String = "",
    val entries: List<BattlePackEntry>
)

@Serializable
data class BattlePackEntry(
    val technologyName: String,
    val topicName: String,
    val topicDescription: String = "",
    val questions: List<BattlePackQuestion>
)

@Serializable
data class BattlePackQuestion(
    val questionText: String,
    val difficulty: String = "BEGINNER",
    val explanation: String,
    val answers: List<BattlePackAnswer>
)

@Serializable
data class BattlePackAnswer(
    val answerText: String,
    val isCorrect: Boolean = false
)
