package com.elkrrai.techtalk.data.remote

import com.elkrrai.techtalk.data.remote.firebase.BattleStatus
import com.elkrrai.techtalk.data.remote.firebase.FirebaseMatchAnswerOption
import com.elkrrai.techtalk.data.remote.firebase.FirebaseMatchDoc
import com.elkrrai.techtalk.data.remote.firebase.FirebaseMatchQuestion
import com.elkrrai.techtalk.data.remote.firebase.FirebaseRoomDoc
import com.elkrrai.techtalk.data.utils.currentEpochMillis
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.common.getPoints
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
    const val GUEST_AVATAR_KEY = "guestAvatarKey"
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
 * Game format: each player races through the SAME fixed [FirebaseMatchDoc.questions]
 * list independently (their own pace, not turn-based) — so [submitAnswer] never needs a
 * round trip before the next question: the next [OnlineBattleEvent.QuestionPushed] is
 * built directly from that already-synced list the instant a write succeeds. The full
 * question content is resolved once, from the HOST's local database, at match start —
 * see [FirebaseMatchDoc.questions]' doc comment for why a plain shared id isn't safe.
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
    private var playerAvatarKey: String = ""
    private var isHost: Boolean = false

    private var currentRoomCode: String? = null
    private var currentMatchId: String? = null
    private var answeredCount: Int = 0

    private var roomJob: Job? = null
    private var matchJob: Job? = null

    // Emit MatchStarted at most once per matchId for this process's lifetime, no matter
    // how many times listenToMatch gets re-attached for it — see listenToMatch's own
    // comment for why re-attachment (by design) must still happen repeatedly.
    private val announcedMatchIds = mutableSetOf<String>()

    override suspend fun connect(playerName: String, avatarKey: String) {
        this.playerName = playerName
        this.playerAvatarKey = avatarKey
        runCatching {
            Firebase.auth.currentUser?.uid ?: Firebase.auth.signInAnonymously().user?.uid
        }.onSuccess { uid ->
            log("connect: success uid=$uid")
            if (uid == null) {
                _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketNotConnected, "Anonymous sign-in returned no user"))
                return
            }
            playerId = uid
            _events.emit(OnlineBattleEvent.Connected(playerId = uid, sessionId = uid, matchStartAtMillis = null))
        }.onFailure {
            log("connect: FAILED ${it::class.simpleName}: ${it.message}")
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
        // currentMatchId is deliberately NOT set here — it's assigned only once
        // listenToRoom actually attaches to the match (status flips to in_progress).
        // Pre-setting it to this room's matchId made that attachment's own guard
        // condition (`currentMatchId != room.matchId`) false from the very first
        // snapshot, so listenToMatch() was never called and the match never progressed
        // past the lobby for either side — see the room-listener trigger below.

        val doc = FirebaseRoomDoc(
            matchId = matchId,
            hostId = uid,
            hostName = playerName,
            hostAvatarKey = playerAvatarKey,
            technologyId = settings.technologyId,
            technologyName = settings.technologyName,
            difficulty = settings.difficulty.name,
            timeControl = settings.timeControl.name,
            status = BattleStatus.Waiting.key,
            createdAtEpochMillis = currentEpochMillis()
        )
        val failed = runCatching { Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").setValue(doc) }
            .onFailure { log("createRoom: setValue FAILED ${it::class.simpleName}: ${it.message}") }
            .isFailure
        if (failed) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Failed to create room"))
            return
        }
        log("createRoom: OK roomCode=$roomCode matchId=$matchId hostId=$uid")
        _events.emit(OnlineBattleEvent.RoomCreated(roomCode = roomCode, matchId = matchId, expiresInSeconds = ROOM_EXPIRY_SECONDS))
        listenToRoom(roomCode)
    }

    override suspend fun joinRoom(request: JoinOnlineRoomRequest) {
        val uid = playerId ?: return emitNotConnected()
        val roomRef = Firebase.database.reference("${FirebasePath.ROOMS}/${request.roomCode}")
        val room = runCatching { roomRef.valueEvents.first().value<FirebaseRoomDoc>() }
            .onFailure { log("joinRoom: read FAILED ${it::class.simpleName}: ${it.message}") }
            .getOrNull()
        log("joinRoom: roomCode=${request.roomCode} read hostId=${room?.hostId} guestId=${room?.guestId} status=${room?.status}")
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
        // currentMatchId intentionally left unset here too — see the comment in
        // createRoom().
        val failed = runCatching {
            roomRef.updateChildren(
                mapOf(RoomKey.GUEST_ID to uid, RoomKey.GUEST_NAME to playerName, RoomKey.GUEST_AVATAR_KEY to playerAvatarKey)
            )
        }.onFailure { log("joinRoom: updateChildren FAILED ${it::class.simpleName}: ${it.message}") }
            .isFailure
        if (failed) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "Failed to join room"))
            return
        }
        log("joinRoom: OK roomCode=${request.roomCode} guestId=$uid")
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
        // Graded against the match doc's own synced questions, not a local database
        // lookup — see FirebaseMatchDoc.questions' doc comment for why a local lookup
        // by id is unsafe here.
        val current = runCatching { matchRef.valueEvents.first().value<FirebaseMatchDoc>() }.getOrNull()
        val matchedQuestion = current?.questions?.firstOrNull { it.questionId == request.questionId }
        val correctAnswer = matchedQuestion?.options?.firstOrNull { it.answerId == request.answerId }
        val isCorrect = correctAnswer?.isCorrect == true
        // Points scale with the question's own difficulty (Difficulty.getPoints():
        // BEGINNER=1, INTERMEDIATE=3, ADVANCED=5) instead of a flat value, matching how
        // offline battles already score.
        val questionDifficulty = runCatching { Difficulty.valueOf(matchedQuestion?.difficulty ?: "") }.getOrDefault(Difficulty.RANDOM)
        val gainedPoints = if (isCorrect) questionDifficulty.getPoints() else 0
        answeredCount += 1

        val scoreField = if (isHost) MatchKey.HOST_SCORE else MatchKey.GUEST_SCORE
        val countField = if (isHost) MatchKey.HOST_ANSWERED_COUNT else MatchKey.GUEST_ANSWERED_COUNT
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

        val questions = current?.questions.orEmpty()
        if (answeredCount < questions.size) {
            emitNextQuestion(matchId, questions, answeredCount)
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
                        mapOf(
                            RoomKey.GUEST_ID to null,
                            RoomKey.GUEST_NAME to null,
                            RoomKey.GUEST_AVATAR_KEY to null,
                            RoomKey.GUEST_READY to false
                        )
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
                val host = OnlinePlayer(room.hostId, room.hostName, room.hostAvatarKey)
                val guest = room.guestId?.let { OnlinePlayer(it, room.guestName.orEmpty(), room.guestAvatarKey.orEmpty()) }

                log(
                    "listenToRoom[$roomCode] isHost=$isHost hostId=${room.hostId} guestId=${room.guestId} " +
                        "status=${room.status} matchId=${room.matchId} currentMatchId=$currentMatchId"
                )

                val guestJustJoined = previous?.guestId == null && room.guestId != null
                if (guestJustJoined) {
                    _events.emit(OnlineBattleEvent.RoomJoined(roomCode, room.matchId, host, guest, settings))
                } else {
                    _events.emit(OnlineBattleEvent.LobbyUpdated(roomCode, host, guest, room.hostReady, room.guestReady, settings))
                }

                if (isHost && guest != null && room.status == BattleStatus.Waiting.key) {
                    log("listenToRoom[$roomCode]: conditions met, calling startMatch")
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
                log("listenToMatch[$matchId]: status=${match.status} questionCount=${match.questions.size}")
                if (!announcedStart) {
                    announcedStart = true
                    if (announcedMatchIds.add(matchId)) {
                        _events.emit(OnlineBattleEvent.MatchStarted(matchId, match.startedAtEpochMillis, match.totalDurationSeconds))
                    } else {
                        log("listenToMatch[$matchId]: MatchStarted already announced, skipping re-emit")
                    }
                    emitNextQuestion(matchId, match.questions, answeredCount)
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
        val guestId = room.guestId ?: run {
            log("startMatch: aborted, guestId is null")
            return
        }
        val questionIds = if (settings.difficulty == Difficulty.RANDOM) {
            repository.getQuestionIdsByTechnology(settings.technologyId)
        } else {
            repository.getQuestionIdsByTechnologyAndDifficulty(settings.technologyId, settings.difficulty)
        }.shuffled().take(MATCH_QUESTION_COUNT)

        log("startMatch: technologyId=${settings.technologyId} difficulty=${settings.difficulty} questionIds.size=${questionIds.size}")
        if (questionIds.isEmpty()) {
            log("startMatch: aborted, no questions available")
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "No questions available for this technology"))
            return
        }
        // Resolved from the HOST's own local database, once, here — the guest never
        // needs to look anything up locally afterward. See FirebaseMatchDoc.questions.
        val questions = questionIds.mapNotNull { id ->
            val question = repository.getQuestionById(id) ?: return@mapNotNull null
            val answers = repository.getAnswersByQuestionId(id)
            FirebaseMatchQuestion(
                questionId = id.toString(),
                prompt = question.questionText,
                difficulty = question.difficulty.name,
                options = answers.map {
                    FirebaseMatchAnswerOption(answerId = it.id.toString(), text = it.answerText, isCorrect = it.isCorrect)
                }
            )
        }
        log("startMatch: resolved questions.size=${questions.size}")
        if (questions.isEmpty()) {
            log("startMatch: aborted, resolved question content is empty")
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, "No questions available for this technology"))
            return
        }
        val matchDoc = FirebaseMatchDoc(
            roomCode = roomCode,
            hostId = room.hostId,
            guestId = guestId,
            startedAtEpochMillis = currentEpochMillis(),
            totalDurationSeconds = settings.timeControl.totalSeconds.takeIf { it > 0 },
            questions = questions
        )
        runCatching {
            Firebase.database.reference("${FirebasePath.MATCHES}/${room.matchId}").setValue(matchDoc)
            Firebase.database.reference("${FirebasePath.ROOMS}/$roomCode").updateChildren(mapOf(RoomKey.STATUS to BattleStatus.InProgress.key))
        }.onFailure {
            log("startMatch: write FAILED ${it::class.simpleName}: ${it.message}")
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, it.message ?: "Failed to start match"))
            return
        }
        log("startMatch: OK matchId=${room.matchId}, wrote match doc + room status=in_progress")
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

    /** Built entirely from the already-synced [FirebaseMatchQuestion] — no local
     * database lookup, so this works identically for host and guest. `isCorrect` is
     * deliberately dropped before exposing [OnlineAnswerOption] to the caller. */
    private suspend fun emitNextQuestion(matchId: String, questions: List<FirebaseMatchQuestion>, index: Int) {
        val question = questions.getOrNull(index) ?: return
        val payload = OnlineQuestionPayload(
            questionId = question.questionId,
            prompt = question.prompt,
            difficulty = runCatching { Difficulty.valueOf(question.difficulty) }.getOrDefault(Difficulty.RANDOM),
            options = question.options.map { OnlineAnswerOption(answerId = it.answerId, text = it.text) }
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
        val total = current?.questions?.size ?: return
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

    // TEMPORARY diagnostic logging for the "stuck waiting" online-battle bug — remove
    // once resolved. `println` rather than a logging library since none exists in this
    // project yet; on Android it surfaces in logcat under tag "System.out", filterable
    // with `adb logcat | grep FirebaseOnlineBattle`.
    private fun log(message: String) {
        println("[FirebaseOnlineBattle] $message")
    }

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
                    log("collectCatching: action FAILED ${e::class.simpleName}: ${e.message}")
                    _events.emit(OnlineBattleEvent.Failure(FailureCode.ProtocolDecodeError, e.message ?: "Listener error"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log("collectCatching: listener FAILED ${e::class.simpleName}: ${e.message}")
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, e.message ?: "Listener closed"))
        }
    }

    private companion object {
        const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I — easier to read aloud
    }
}
