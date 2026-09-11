package com.elkrrai.techtalk.presentation.battle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.elkrrai.techtalk.presentation.battle.home.BattleHomeScreen
import com.elkrrai.techtalk.presentation.battle.home.BattleHomeViewModel
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleScreen
import com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleViewModel
import com.elkrrai.techtalk.presentation.battle.online.BattleLobbyScreen
import com.elkrrai.techtalk.presentation.battle.online.BattleLobbyViewModel
import com.elkrrai.techtalk.presentation.battle.online.OnlineBattleScreen
import com.elkrrai.techtalk.presentation.battle.online.OnlineBattleViewModel
import com.elkrrai.techtalk.presentation.battle.result.BattleResultScreen
import com.elkrrai.techtalk.presentation.battle.result.BattleResultViewModel
import com.elkrrai.techtalk.presentation.battle.state.BattlePhase
import com.elkrrai.techtalk.presentation.battle.state.BattleSessionStore
import com.elkrrai.techtalk.presentation.battle.state.OnlineBattlePhase
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Phase router for the Battle tab — owns no state of its own beyond what it reads
 * from the injected [BattleSessionStore] singleton. */
@Composable
fun BattleScreen(modifier: Modifier = Modifier) {
    val sessionStore: BattleSessionStore = koinInject()
    val session by sessionStore.state.collectAsState()

    when (session.phase) {
        BattlePhase.HOME -> BattleHomeScreen(
            viewModel = koinViewModel<BattleHomeViewModel>(),
            modifier = modifier
        )

        BattlePhase.BATTLE -> when (session.mode) {
            BattleMode.OFFLINE -> OfflineBattleScreen(
                viewModel = koinViewModel<OfflineBattleViewModel>(),
                modifier = modifier
            )
            BattleMode.ONLINE -> when (session.onlinePhase) {
                OnlineBattlePhase.LOBBY -> BattleLobbyScreen(
                    viewModel = koinViewModel<BattleLobbyViewModel>(),
                    modifier = modifier
                )
                OnlineBattlePhase.MATCH -> OnlineBattleScreen(
                    viewModel = koinViewModel<OnlineBattleViewModel>(),
                    modifier = modifier
                )
            }
        }

        BattlePhase.RESULT -> BattleResultScreen(
            viewModel = koinViewModel<BattleResultViewModel>(),
            onClose = sessionStore::reset,
            modifier = modifier
        )
    }
}
