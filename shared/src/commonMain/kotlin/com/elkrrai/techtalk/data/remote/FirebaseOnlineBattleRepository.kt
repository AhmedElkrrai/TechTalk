package com.elkrrai.techtalk.data.remote

import com.elkrrai.techtalk.data.remote.firebase.BattleStatus
import com.elkrrai.techtalk.data.remote.firebase.FirebaseMatchDoc
import com.elkrrai.techtalk.data.remote.firebase.FirebaseRoomDoc
import com.elkrrai.techtalk.data.utils.currentEpochMillis
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.model.online.FailureCode
import com.elkrrai.techtalk.domain.model.online.JoinOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.LeaveOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.OnlineAnswerOption
import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlineMatchEndReason
import com.elkrrai.techtalk.domain.model.online.OnlineMatchSettings
import com.elkrrai.techtalk.domain.model.online.OnlinePlayer
import com.elkrrai.techtalk.domain.model.online.OnlineQuestionPayload
import com.elkrrai.techtalk.domain.model.online.OnlineScoreBoard
import com.elkrrai.techtalk.domain.model.online.ReconnectOnlineBattleRequest
import com.elkrrai.techtalk.domain.model.online.RequestOnlineRematchRequest
import com.elkrrai.techtalk.domain.model.online.SetOnlinePlayerReadyRequest
import com.elkrrai.techtalk.domain.model.online.SubmitOnlineAnswerRequest
import com.elkrrai.techtalk.domain.repository.OnlineBattleRepository
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Keep in sync with [com.elkrrai.techtalk.presentation.battle.state.BATTLE_TOTAL_QUESTIONS]
 * — duplicated locally rather than imported so the data layer doesn't reach into a
 * presentation-layer package. */
private const val MATCH_QUESTION_COUNT = 10
private const val ROOM_EXPIRY_SECONDS = 600

/** Realtime Database top-level path segments. */
private object FirebasePath {
    const val ROOMS = "rooms"
    const val MATCHES = "matches"
    const val ANSWERS = "answers"
}

/** `rooms/{roomCode}` field keys — kept as named constants instead of raw strings
 * since [DatabaseReference.updateChildren] takes an untyped `Map<String, Any?>`, with
 * no compiler check against [FirebaseRoomDoc]'s actual property names. */
private object RoomKey {
    const val MATCH_ID = "matchId"
    const val GUEST_ID = "guestId"
    const val GUEST_NAME = "guestName"
    const val HOST_READY = "hostReady"
    const val GUEST_READY = "guestReady"
    const val STATUS = "status"
    const val REMATCH_REQUESTED_BY_HOST = "rematchRequestedByHost"
    const val REMATCH_REQUESTED_BY_GUEST = "rematchRequestedByGuest"
}

/** `matches/{matchId}` field keys — see [RoomKey]. */
private object MatchKey {
    const val HOST_SCORE = "hostScore"
    const val GUEST_SCORE = "guestScore"
    const val HOST_ANSWERED_COUNT = "hostAnsweredCount"
    const val GUEST_ANSWERED_COUNT = "guestAnsweredCount"
    const val STATUS = "status"
    const val WINNER_PLAYER_ID = "winnerPlayerId"
    const val END_REASON = "endReason"
}

/** `matches/{matchId}/answers/{playerId}/{questionId}` field keys — see [RoomKey]. */
private object AnswerKey {
    const val ANSWER_ID = "answerId"
    const val IS_CORRECT = "isCorrect"
    const val GAINED_POINTS = "gainedPoints"
    const val ANSWERED_AT_EPOCH_MILLIS = "answeredAtEpochMillis"
}

/**
 * Realtime-Database-backed implementation of [OnlineBattleRepository] — a server-less
 * alternative to [KtorOnlineBattleRepository]. Every "server push" in the Ktor version
 * becomes a listener on a shared `rooms/{code}` or `matches/{id}` path here; every
 * "client command" becomes a write. See the design discussion this came out of for the
 * full data-shape rationale.
 *
 * Game format: each player races through the SAME fixed [FirebaseMatchDoc.questionIds]
 * list independently (their own pace, not turn-based) — so [submitAnswer] never needs a
 * round trip before the next question: the next [OnlineBattleEvent.QuestionPushed] is
 * built locally from [repository]'s own local content the instant a write succeeds.
 * Both players already have the same bundled content pack, so nothing about question
 * text or answer options is ever written to Firebase at all.
 *
 * Read this before treating it as more than a portfolio-grade first pass:
 * - **No authoritative grading.** Each client reports its own `isCorrect`/score; there is
 *   no server to validate either, so a modified client can cheat. A real competitive
 *   product would need a Cloud Function (or actual backend) as the trusted grader.
 * - **No per-second `TimerTick` push.** Callers should derive their own countdown from
 *   [OnlineBattleEvent.MatchStarted]'s `startedAtEpochMillis` + `totalDurationSeconds`
 *   instead of expecting this repository to emit one.
 * - [requestRematch] and [reconnect] are minimal first passes — see their doc comments.
 * - [joinRoom]'s guest-slot claim is read-then-write, not a real compare-and-swap
 *   transaction — an accepted, low-probability race for portfolio-scale traffic.
 */
class FirebaseOnlineBattleRepository(
    private val repository: TechTalkRepository
) : OnlineBattleRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _events = MutableSharedFlow<OnlineBattleEvent>(extraBufferCapacity = 64)
    override val events: Flow<OnlineBattleEvent> = _events

    private var playerId: String? = null
    private var playerName: String = "Player"
    private var isHost: Boolean = false

    private var currentRoomCode: String? = null
    private var currentMatchId: String? = null
    private var answeredCount: Int = 0

    private var roomJob: Job? = null
    private var matchJob: Job? = null

    override suspend fun connect(playerName: String) {
        this.playerName = playerName
        runCatching {
            Firebase.auth.currentUser?.uid ?: Firebase.auth.signInAnonymously().user?.uid
        }.onSuccess { uid ->
            if (uid == null) {
                _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketNotConnected, "Anonymous sign-in returned no user"))
                return
            }
            playerId = uid
            _events.emit(OnlineBattleEvent.Connected(playerId = uid, sessionId = uid, matchStartAtMillis = null))
        }.onFailure {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketNotConnected, it.message ?: "Sign-in failed"))
        }
    }

    override suspend fun disconnect() {
        roomJob?.cancel(); roomJob = null
        matchJob?.cancel(); matchJob = null
        currentRoomCode = null
        currentMatchId = null
        _events.emit(OnlineBattleEvent.Disconnected)
    }

    override suspend fun createRoom(settings: OnlineMatchSettings) {
        val uid = playerId ?: return emitNotConnected()
        val roomCode = newCode()
        val matchId = newId("match")
        isHost = true
        currentRoomCode = roomCode
        currentMatchId = matchId

        val doc = FirebaseRoomDoc(
            matchId = matchId,
            hostId = uid,
            hostName = playerName,
            technologyId = settings.technologyId,
            technologyName = settings.technologyName,
            difficulty = settings.difficulty.name,
            timeControl = settings.timeControl.name,
            status = BattleStatus.Waiting.key,
            createdAtEpochMillis = currentEpochMillis()
        )
        val failed = runCatching { Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").setValue(doc) }.isFailure
        if (failed) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Failed to create room"))
            return
        }
        _events.emit(OnlineBattleEvent.RoomCreated(roomCode = roomCode, matchId = matchId, expiresInSeconds = ROOM_EXPIRY_SECONDS))
        listenToRoom(roomCode)
    }

    override suspend fun joinRoom(request: JoinOnlineRoomRequest) {
        val uid = playerId ?: return emitNotConnected()
        val roomRef = Firebase.database.reference("${FirebasePath.ROOMS}/${request.roomCode}")
        val room = runCatching { roomRef.valueEvents.first().value<FirebaseRoomDoc>() }.getOrNull()
        if (room == null || room.hostId.isBlank()) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Room not found"))
            return
        }
        if (room.guestId != null) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Room is full"))
            return
        }
        isHost = false
        currentRoomCode = request.roomCode
        currentMatchId = room.matchId
        val failed = runCatching {
            roomRef.updateChildren(mapOf(RoomKey.GUEST_ID to uid, RoomKey.GUEST_NAME to playerName))
        }.isFailure
        if (failed) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Failed to join room"))
            return
        }
        listenToRoom(request.roomCode)
    }

    override suspend fun setPlayerReady(request: SetOnlinePlayerReadyRequest) {
        val roomCode = currentRoomCode ?: return emitNotConnected()
        val field = if (isHost) RoomKey.HOST_READY else RoomKey.GUEST_READY
        runCatching {
            Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").updateChildren(mapOf(field to request.isReady))
        }.onFailure { _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, it.message ?: "Failed to set ready")) }
    }

    override suspend fun submitAnswer(request: SubmitOnlineAnswerRequest) {
        val uid = playerId ?: return emitNotConnected()
        val matchId = currentMatchId ?: return emitNotConnected()
        val matchRef = Firebase.database.reference("${FirebasePath.MATCHES}/$matchId")
        val questionId = request.questionId.toLongOrNull()
        val correctAnswer = questionId?.let { id ->
            repository.getAnswersByQuestionId(id).firstOrNull { it.id.toString() == request.answerId }
        }
        val isCorrect = correctAnswer?.isCorrect == true
        val gainedPoints = if (isCorrect) 10 else 0
        answeredCount += 1

        val scoreField = if (isHost) MatchKey.HOST_SCORE else MatchKey.GUEST_SCORE
        val countField = if (isHost) MatchKey.HOST_ANSWERED_COUNT else MatchKey.GUEST_ANSWERED_COUNT
        val current = runCatching { matchRef.valueEvents.first().value<FirebaseMatchDoc>() }.getOrNull()
        val newScore = (if (isHost) current?.hostScore else current?.guestScore)?.plus(gainedPoints) ?: gainedPoints

        val failed = runCatching {
            matchRef.updateChildren(
                mapOf(
                    scoreField to newScore,
                    countField to answeredCount,
                    "${FirebasePath.ANSWERS}/$uid/${request.questionId}" to mapOf(
                        AnswerKey.ANSWER_ID to request.answerId,
                        AnswerKey.IS_CORRECT to isCorrect,
                        AnswerKey.GAINED_POINTS to gainedPoints,
                        AnswerKey.ANSWERED_AT_EPOCH_MILLIS to currentEpochMillis()
                    )
                )
            )
        }.isFailure
        if (failed) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Failed to submit answer"))
            return
        }

        val scoreboard = current.toScoreboard(isHostUpdate = isHost, newScore = newScore)
        _events.emit(OnlineBattleEvent.AnswerResult(matchId, request.questionId, isCorrect, gainedPoints, scoreboard))

        val questionIds = current?.questionIds.orEmpty()
        if (answeredCount < questionIds.size) {
            emitNextQuestion(matchId, questionIds, answeredCount)
        } else {
            maybeFinishMatch(matchRef, current, isHostFinished = isHost)
        }
    }

    /** Minimal first pass: flips a per-side flag; once both are set, the HOST client's
     * room listener rebuilds the room into a fresh "waiting" lobby with a new matchId,
     * re-using the same room code so both players land back in the same lobby screen.
     * There's no dedicated "rematch accepted" event — callers see it as an ordinary
     * [OnlineBattleEvent.LobbyUpdated] on the room they're already listening to. */
    override suspend fun requestRematch(request: RequestOnlineRematchRequest) {
        val roomCode = currentRoomCode ?: return emitNotConnected()
        val field = if (isHost) RoomKey.REMATCH_REQUESTED_BY_HOST else RoomKey.REMATCH_REQUESTED_BY_GUEST
        runCatching {
            Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").updateChildren(mapOf(field to true))
        }.onFailure { _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, it.message ?: "Failed to request rematch")) }
    }

    override suspend fun leaveRoom(request: LeaveOnlineRoomRequest) {
        val roomCode = currentRoomCode
        matchJob?.cancel(); matchJob = null
        roomJob?.cancel(); roomJob = null
        if (roomCode != null) {
            runCatching {
                if (isHost) {
                    Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").removeValue()
                } else {
                    Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").updateChildren(
                        mapOf(RoomKey.GUEST_ID to null, RoomKey.GUEST_NAME to null, RoomKey.GUEST_READY to false)
                    )
                }
            }
        }
        currentRoomCode = null
        currentMatchId = null
        answeredCount = 0
    }

    /** Minimal first pass: [ReconnectOnlineBattleRequest.playerToken] isn't checked
     * against anything — there's no server to issue/verify one under anonymous auth —
     * this just re-attaches listeners using [ReconnectOnlineBattleRequest.matchId] to
     * look up the room code, so an app relaunch resumes the same match. */
    override suspend fun reconnect(request: ReconnectOnlineBattleRequest) {
        val matchRef = Firebase.database.reference("${FirebasePath.MATCHES}/${request.matchId}")
        val match = runCatching { matchRef.valueEvents.first().value<FirebaseMatchDoc>() }.getOrNull()
        if (match == null || match.roomCode.isBlank()) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Match not found"))
            return
        }
        playerId = request.playerId
        isHost = match.hostId == request.playerId
        currentRoomCode = match.roomCode
        currentMatchId = request.matchId
        listenToRoom(match.roomCode)
        listenToMatch(request.matchId)
    }

    /** Cancels the internal scope — call when the app/session tears down. */
    fun clear() {
        scope.cancel()
    }

    // ---- listeners -----------------------------------------------------------------

    private fun listenToRoom(roomCode: String) {
        roomJob?.cancel()
        roomJob = scope.launch {
            var previous: FirebaseRoomDoc? = null
            Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").valueEvents.collectCatching { snapshot ->
                val room = snapshot.value<FirebaseRoomDoc?>()
                if (room == null) {
                    _events.emit(OnlineBattleEvent.RoomExpired(roomCode, "Room closed"))
                    return@collectCatching
                }
                val settings = OnlineMatchSettings(
                    timeControl = runCatching { BattleTimeControl.valueOf(room.timeControl) }.getOrDefault(BattleTimeControl.ONE_MINUTE),
                    difficulty = runCatching { Difficulty.valueOf(room.difficulty) }.getOrDefault(Difficulty.RANDOM),
                    technologyId = room.technologyId,
                    technologyName = room.technologyName
                )
                val host = OnlinePlayer(room.hostId, room.hostName)
                val guest = room.guestId?.let { OnlinePlayer(it, room.guestName.orEmpty()) }

                val guestJustJoined = previous?.guestId == null && room.guestId != null
                if (guestJustJoined) {
                    _events.emit(OnlineBattleEvent.RoomJoined(roomCode, room.matchId, host, guest, settings))
                } else {
                    _events.emit(OnlineBattleEvent.LobbyUpdated(roomCode, host, guest, room.hostReady, room.guestReady, settings))
                }

                val bothReady = room.hostReady && room.guestReady && guest != null
                if (isHost && bothReady && room.status == BattleStatus.Waiting.key) {
                    startMatch(roomCode, room, settings)
                }
                if (isHost && room.rematchRequestedByHost && room.rematchRequestedByGuest) {
                    rebuildRoomForRematch(roomCode)
                }
                // Both host and guest pick up a fresh match the same way — off the room
                // doc's own status flip, not off whoever wrote it — so a rematch (which
                // stamps a new matchId onto the SAME room) is handled identically to the
                // very first match, no separate code path needed for either side.
                if (room.status == BattleStatus.InProgress.key && room.matchId.isNotBlank() && currentMatchId != room.matchId) {
                    currentMatchId = room.matchId
                    answeredCount = 0
                    listenToMatch(room.matchId)
                }
                previous = room
            }
        }
    }

    private fun listenToMatch(matchId: String) {
        matchJob?.cancel()
        matchJob = scope.launch {
            var announcedStart = false
            Firebase.database.reference("${FirebasePath.MATCHES}/$matchId").valueEvents.collectCatching { snapshot ->
                val match = snapshot.value<FirebaseMatchDoc?>() ?: return@collectCatching
                if (!announcedStart) {
                    announcedStart = true
                    _events.emit(OnlineBattleEvent.MatchStarted(matchId, match.startedAtEpochMillis, match.totalDurationSeconds))
                    emitNextQuestion(matchId, match.questionIds, answeredCount)
                }
                _events.emit(OnlineBattleEvent.ScoreUpdated(matchId, match.toScoreboard()))
                if (match.status == BattleStatus.Ended.key) {
                    _events.emit(
                        OnlineBattleEvent.MatchEnded(
                            matchId = matchId,
                            winnerPlayerId = match.winnerPlayerId,
                            endReason = runCatching { OnlineMatchEndReason.valueOf(match.endReason.orEmpty()) }.getOrDefault(OnlineMatchEndReason.UNKNOWN),
                            scoreboard = match.toScoreboard()
                        )
                    )
                }
            }
        }
    }

    // ---- match lifecycle helpers -----------------------------------------------------

    /** Only the host writes this — both clients are listening to the same room doc, so
     * only one of them may act on "both ready" or the match doc gets written twice. */
    private suspend fun startMatch(roomCode: String, room: FirebaseRoomDoc, settings: OnlineMatchSettings) {
        val guestId = room.guestId ?: return
        val questionIds = if (settings.difficulty == Difficulty.RANDOM) {
            repository.getQuestionIdsByTechnology(settings.technologyId)
        } else {
            repository.getQuestionIdsByTechnologyAndDifficulty(settings.technologyId, settings.difficulty)
        }.shuffled().take(MATCH_QUESTION_COUNT)

        if (questionIds.isEmpty()) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "No questions available for this technology"))
            return
        }
        val matchDoc = FirebaseMatchDoc(
            roomCode = roomCode,
            hostId = room.hostId,
            guestId = guestId,
            startedAtEpochMillis = currentEpochMillis(),
            totalDurationSeconds = settings.timeControl.totalSeconds.takeIf { it > 0 },
            questionIds = questionIds
        )
        runCatching {
            Firebase.database.reference("${FirebasePath.MATCHES}/${room.matchId}").setValue(matchDoc)
            Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").updateChildren(mapOf(RoomKey.STATUS to BattleStatus.InProgress.key))
        }.onFailure {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, it.message ?: "Failed to start match"))
            return
        }
        _events.emit(OnlineBattleEvent.MatchStarting(room.matchId, currentEpochMillis(), countdownMillis = 0))
        // Deliberately doesn't call listenToMatch() itself — the room listener above
        // picks up this write's own "in_progress" status on its very next callback and
        // attaches to the match from there, the same way the guest does.
    }

    private suspend fun rebuildRoomForRematch(roomCode: String) {
        val freshMatchId = newId("match")
        runCatching {
            Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").updateChildren(
                mapOf(
                    RoomKey.MATCH_ID to freshMatchId,
                    RoomKey.STATUS to BattleStatus.Waiting.key,
                    RoomKey.HOST_READY to false,
                    RoomKey.GUEST_READY to false,
                    RoomKey.REMATCH_REQUESTED_BY_HOST to false,
                    RoomKey.REMATCH_REQUESTED_BY_GUEST to false
                )
            )
        }
    }

    private suspend fun emitNextQuestion(matchId: String, questionIds: List<Long>, index: Int) {
        val questionId = questionIds.getOrNull(index) ?: return
        val question = repository.getQuestionById(questionId) ?: return
        val answers = repository.getAnswersByQuestionId(questionId)
        val payload = OnlineQuestionPayload(
            questionId = questionId.toString(),
            prompt = question.questionText,
            difficulty = question.difficulty,
            options = answers.map { OnlineAnswerOption(answerId = it.id.toString(), text = it.answerText) }
        )
        _events.emit(OnlineBattleEvent.QuestionPushed(matchId, index, payload))
    }

    /** "First writer wins": whichever client sees both sides fully answered writes
     * `status = "ended"` guarded by the read-then-write pattern already used elsewhere
     * here — a real backend would arbitrate this instead of trusting either client. */
    private suspend fun maybeFinishMatch(
        matchRef: DatabaseReference,
        current: FirebaseMatchDoc?,
        isHostFinished: Boolean
    ) {
        val total = current?.questionIds?.size ?: return
        val opponentCount = if (isHostFinished) current.guestAnsweredCount else current.hostAnsweredCount
        if (opponentCount < total) return // opponent still playing — wait for their write to trigger this check
        val winnerPlayerId = when {
            current.hostScore > current.guestScore -> current.hostId
            current.guestScore > current.hostScore -> current.guestId
            else -> null
        }
        runCatching {
            matchRef.updateChildren(
                mapOf(
                    MatchKey.STATUS to BattleStatus.Ended.key,
                    MatchKey.WINNER_PLAYER_ID to winnerPlayerId,
                    MatchKey.END_REASON to OnlineMatchEndReason.QUESTIONS_EXHAUSTED.name
                )
            )
        }
    }

    // ---- small helpers -----------------------------------------------------------

    private fun FirebaseMatchDoc?.toScoreboard(isHostUpdate: Boolean = true, newScore: Int? = null): OnlineScoreBoard {
        val hostScore = if (isHostUpdate) newScore ?: this?.hostScore ?: 0 else this?.hostScore ?: 0
        val guestScore = if (!isHostUpdate) newScore ?: this?.guestScore ?: 0 else this?.guestScore ?: 0
        return OnlineScoreBoard(
            hostScore = hostScore,
            guestScore = guestScore,
            hostPlayerId = this?.hostId.orEmpty(),
            guestPlayerId = this?.guestId.orEmpty()
        )
    }

    private suspend fun emitNotConnected() {
        _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketNotConnected, "Not connected"))
    }

    private fun newCode(): String = (1..6).map { CODE_CHARS.random(Random) }.joinToString("")

    private fun newId(prefix: String): String =
        "${prefix}_" + currentEpochMillis().toString(36) + Random.nextLong().toString(36)

    /** A cancelled job (e.g. [leaveRoom]/[disconnect], or [listenToRoom]/[listenToMatch]
     * replacing a still-running listener) throws [CancellationException] through this
     * `collect` — that's normal, cooperative cancellation, not a connection problem, so
     * it's rethrown rather than reported as an [OnlineBattleEvent.Failure]. Swallowing it
     * here previously surfaced raw coroutine-internal text ("StandaloneCoroutine was
     * cancelled") to the player as a bogus "Connection error". */
    private suspend inline fun <T> Flow<T>.collectCatching(crossinline action: suspend (T) -> Unit) {
        try {
            collect { value ->
                try {
                    action(value)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _events.emit(OnlineBattleEvent.Failure(FailureCode.ProtocolDecodeError, e.message ?: "Listener error"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, e.message ?: "Listener closed"))
        }
    }

    private companion object {
        const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I — easier to read aloud
    }
}
