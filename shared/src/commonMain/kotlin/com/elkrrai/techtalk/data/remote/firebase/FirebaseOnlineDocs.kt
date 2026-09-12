package com.elkrrai.techtalk.data.remote.firebase

import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.common.getTitle
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import kotlinx.serialization.Serializable

/**
 * Realtime Database documents for [com.elkrrai.techtalk.data.remote.FirebaseOnlineBattleRepository].
 * Every field has a default so GitLive's kotlinx.serialization-based `DataSnapshot.value<T>()`
 * can decode a partially-written node without throwing. Stored at `rooms/{roomCode}`.
 */
@Serializable
data class FirebaseRoomDoc(
    val matchId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val guestId: String? = null,
    val guestName: String? = null,
    val hostReady: Boolean = false,
    val guestReady: Boolean = false,
    val technologyId: Long = 0,
    val technologyName: String = "",
    val difficulty: String = Difficulty.RANDOM.name ,
    val timeControl: String = BattleTimeControl.ONE_MINUTE.name,
    val status: String = BattleStatus.Waiting.key,
    val createdAtEpochMillis: Long = 0,
    val rematchRequestedByHost: Boolean = false,
    val rematchRequestedByGuest: Boolean = false
)

/**
 * Stored at `matches/{matchId}` — the WHOLE match (config + live score) lives in this
 * one node, read and written by both players, rather than split across several paths.
 * [questionIds] are looked up by each client against its OWN local content database
 * (same bundled pack on both devices) — nothing about question text or answer options
 * is ever written here, so delivering a question costs zero extra Firebase round-trips.
 * `answers/{playerId}/{questionId}` is written as a raw nested map by
 * [com.elkrrai.techtalk.data.remote.FirebaseOnlineBattleRepository.submitAnswer] for
 * audit purposes only — nothing reads it back, so it has no matching data class here.
 */
@Serializable
data class FirebaseMatchDoc(
    val roomCode: String = "",
    val hostId: String = "",
    val guestId: String = "",
    val startedAtEpochMillis: Long = 0,
    val totalDurationSeconds: Int? = null,
    val questionIds: List<Long> = emptyList(),
    val hostScore: Int = 0,
    val guestScore: Int = 0,
    val hostAnsweredCount: Int = 0,
    val guestAnsweredCount: Int = 0,
    val status: String = BattleStatus.InProgress.key,
    val winnerPlayerId: String? = null,
    val endReason: String? = null
)

enum class BattleStatus(val key: String) {
    Waiting("waiting"),
    InProgress("in_progress"),
    Ended("ended");
}
