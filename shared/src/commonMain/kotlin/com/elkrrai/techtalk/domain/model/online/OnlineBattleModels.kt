package com.elkrrai.techtalk.domain.model.online

import com.elkrrai.techtalk.domain.model.common.Difficulty

/**
 * [INFINITY] uses the sentinel `-1` — never treat [totalSeconds] as a plain duration
 * without checking for it. The data layer also derives its heartbeat interval from
 * this enum (2s / 4s / 5s / 5s).
 */
enum class BattleTimeControl(val totalSeconds: Int) {
    ONE_MINUTE(60),
    TWO_MINUTES(120),
    THREE_MINUTES(180),
    INFINITY(-1)
}

data class OnlinePlayer(
    val playerId: String,
    val displayName: String
)

data class OnlineScoreBoard(
    val hostScore: Int,
    val guestScore: Int,
    val hostPlayerId: String,
    val guestPlayerId: String
)

/**
 * Server-pushed payload, deliberately separate from the local [com.elkrrai.techtalk
 * .domain.model.battle.BattleQuestion]/[com.elkrrai.techtalk.domain.model.battle
 * .BattleAnswer] pair. Online answers carry no `isCorrect` — the server decides.
 */
data class OnlineQuestionPayload(
    val questionId: String,
    val prompt: String,
    val difficulty: Difficulty,
    val options: List<OnlineAnswerOption>
)

data class OnlineAnswerOption(
    val answerId: String,
    val text: String
)

enum class OnlineMatchEndReason {
    TIME_UP,
    QUESTIONS_EXHAUSTED,
    OPPONENT_TIMEOUT,
    ROOM_CLOSED,
    LEFT_ROOM,
    UNKNOWN
}
