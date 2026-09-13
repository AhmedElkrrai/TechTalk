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
    val hostAvatarKey: String = "",
    val guestId: String? = null,
    val guestName: String? = null,
    val guestAvatarKey: String? = null,
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
 *
 * [questions] carries the FULL resolved content (prompt + answer options, [Boolean]
 * correctness included) rather than local database ids — an earlier version shipped
 * just `questionIds: List<Long>` on the theory that "both players have the same bundled
 * content pack, so a plain id is enough for each client to look the question up
 * locally." That's false in practice: Room's `@PrimaryKey(autoGenerate = true)` ids are
 * NOT guaranteed to line up across two independently-seeded on-device databases (a
 * different install history, a different content-pack version at seed time, etc. all
 * shift the sequence) — the guest's local id lookup silently returned nothing and no
 * question ever appeared. Writing the resolved content once, from the host's own
 * database, at match start makes both clients fully self-sufficient afterward: no
 * further local-database dependency, and no possible id mismatch.
 *
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
    val questions: List<FirebaseMatchQuestion> = emptyList(),
    val hostScore: Int = 0,
    val guestScore: Int = 0,
    val hostAnsweredCount: Int = 0,
    val guestAnsweredCount: Int = 0,
    val status: String = BattleStatus.InProgress.key,
    val winnerPlayerId: String? = null,
    val endReason: String? = null
)

@Serializable
data class FirebaseMatchQuestion(
    val questionId: String = "",
    val prompt: String = "",
    val difficulty: String = Difficulty.RANDOM.name,
    val options: List<FirebaseMatchAnswerOption> = emptyList()
)

@Serializable
data class FirebaseMatchAnswerOption(
    val answerId: String = "",
    val text: String = "",
    val isCorrect: Boolean = false
)

enum class BattleStatus(val key: String) {
    Waiting("waiting"),
    InProgress("in_progress"),
    Ended("ended");
}
