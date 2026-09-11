package com.elkrrai.techtalk.presentation.battle.state

import com.elkrrai.techtalk.domain.model.battle.BattleAnswer
import com.elkrrai.techtalk.domain.model.battle.BattleQuestion
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.battle.online.mapper.toOnlineBattleState
import com.elkrrai.techtalk.presentation.technologylist.state.TechnologyUiItem
import com.elkrrai.techtalk.utils.inspect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registered as a Koin singleton, so it outlives every battle ViewModel — the only
 * channel between home -> battle -> result. [BattleState.battleSessionId] is what lets
 * [com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleViewModel] detect "a
 * genuinely new battle started" and restart its timer.
 *
 * Mutation is exposed only through named, self-guarded methods below — never a generic
 * "set any field" function. Each offline/online method only applies if the store's
 * current [BattleState.mode] and [BattleState.phase] actually match the screen calling
 * it; otherwise the write is dropped (and logged) instead of silently corrupting a
 * different screen's in-flight state. This guard enforces *identity* only — "is the
 * caller's screen currently the active one" — not business rules (double-answer guards,
 * last-question checks, etc. stay in the ViewModels, which is where any user-facing
 * consequence of skipping a call belongs).
 *
 * A rejected mutation no-ops rather than throwing: there's no automated test suite for
 * this flow (everything is verified by manual on-device testing), so a thrown exception
 * from a stale coroutine callback — e.g. a timer tick landing one frame after the user
 * backed out — would crash the whole battle flow, which is worse than the bug this
 * guard exists to prevent. It's logged via [inspect] rather than silent, since a
 * silently-dropped write is exactly the kind of invisible bug this exists to catch.
 */
class BattleSessionStore {

    private val _state = MutableStateFlow(BattleState())
    val state: StateFlow<BattleState> = _state.asStateFlow()

    private var nextSessionId = 1

    fun startBattle(state: BattleState) {
        _state.value = state.copy(battleSessionId = nextSessionId)
        nextSessionId++
    }

    // ---- Offline-only mutations (guarded: mode == OFFLINE && phase == BATTLE) ----

    fun tickOfflineTimer(remainingSeconds: Int) =
        mutateOffline("tickOfflineTimer") { it.copy(remainingTimeSeconds = remainingSeconds) }

    fun recordOfflineAnswer(answerId: Long, isCorrect: Boolean) =
        mutateOffline("recordOfflineAnswer") {
            it.copy(
                selectedAnswerId = answerId,
                hasAnswered = true,
                score = if (isCorrect) it.score + 1 else it.score
            )
        }

    fun advanceOfflineQuestion(nextIndex: Int, nextAnswers: List<BattleAnswer>) =
        mutateOffline("advanceOfflineQuestion") {
            it.copy(
                currentQuestionIndex = nextIndex,
                currentAnswers = nextAnswers,
                selectedAnswerId = null,
                hasAnswered = false
            )
        }

    fun finalizeOfflineBattle(xpGained: Int) =
        mutateOffline("finalizeOfflineBattle") { it.copy(phase = BattlePhase.RESULT, xpGained = xpGained) }

    // ---- Online-only mutations (guarded: mode == ONLINE && phase == BATTLE) ----

    fun setCurrentPlayerId(playerId: String) =
        mutateOnline("setCurrentPlayerId") { it.copy(currentPlayerId = playerId) }

    fun transitionToOnlineMatch(totalDurationSeconds: Int?) =
        mutateOnline("transitionToOnlineMatch") { it.toOnlineBattleState(totalDurationSeconds) }

    fun finalizeOnlineBattle(playerScore: Int, xpGained: Int) =
        mutateOnline("finalizeOnlineBattle") {
            it.copy(
                phase = BattlePhase.RESULT,
                onlinePhase = OnlineBattlePhase.LOBBY,
                score = playerScore,
                xpGained = xpGained
            )
        }

    // ---- Result-screen restart ----

    /** Always starts a brand-new OFFLINE battle — `mode` is hardcoded below, not carried
     * over from whatever battle just finished, so this can never hand a leftover ONLINE
     * mode a set of freshly-loaded local questions it has no route that would use. */
    fun restartOfflineBattle(
        technology: TechnologyUiItem,
        subscribedTechnologies: List<TechnologyUiItem>,
        playerName: String,
        playerAvatarKey: String,
        timeControl: BattleTimeControl,
        difficulty: Difficulty,
        questions: List<BattleQuestion>,
        currentAnswers: List<BattleAnswer>
    ) {
        startBattle(
            BattleState(
                phase = BattlePhase.BATTLE,
                subscribedTechnologies = subscribedTechnologies,
                playerName = playerName,
                playerAvatarKey = playerAvatarKey,
                selectedTechnology = technology,
                mode = BattleMode.OFFLINE,
                selectedTimeControl = timeControl,
                selectedDifficulty = difficulty,
                remainingTimeSeconds = if (timeControl == BattleTimeControl.INFINITY) null else timeControl.totalSeconds,
                questions = questions,
                currentAnswers = currentAnswers
            )
        )
    }

    /** Back to defaults; the session id counter also resets to 1. */
    fun reset() {
        nextSessionId = 1
        _state.value = BattleState()
    }

    private fun mutateOffline(caller: String, transform: (BattleState) -> BattleState) {
        val current = _state.value
        if (current.mode != BattleMode.OFFLINE || current.phase != BattlePhase.BATTLE) {
            inspect("blocked (mode=${current.mode}, phase=${current.phase})", tag = caller)
            return
        }
        _state.value = transform(current)
    }

    private fun mutateOnline(caller: String, transform: (BattleState) -> BattleState) {
        val current = _state.value
        if (current.mode != BattleMode.ONLINE || current.phase != BattlePhase.BATTLE) {
            inspect("blocked (mode=${current.mode}, phase=${current.phase})", tag = caller)
            return
        }
        _state.value = transform(current)
    }
}
