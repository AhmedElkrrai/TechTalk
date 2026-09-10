package com.elkrrai.techtalk.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OnlinePlayerDto(
    val playerId: String,
    val displayName: String
)

@Serializable
data class OnlineScoreBoardDto(
    val hostScore: Int,
    val guestScore: Int,
    val hostPlayerId: String,
    val guestPlayerId: String
)

@Serializable
data class OnlineAnswerOptionDto(
    val answerId: String,
    val text: String
)

@Serializable
data class OnlineQuestionPayloadDto(
    val questionId: String,
    val prompt: String,
    val difficulty: String,
    val options: List<OnlineAnswerOptionDto>
)

/** Wire form of [com.elkrrai.techtalk.domain.model.online.OnlineMatchSettings] —
 * enums travel as their `name`. */
@Serializable
data class OnlineMatchSettingsDto(
    val timeControl: String,
    val difficulty: String,
    val technologyId: Long,
    val technologyName: String
)
