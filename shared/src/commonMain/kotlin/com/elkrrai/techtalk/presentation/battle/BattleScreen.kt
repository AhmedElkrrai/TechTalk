package com.elkrrai.techtalk.presentation.battle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.elkrrai.techtalk.presentation.battle.state.BattleState
import com.elkrrai.techtalk.presentation.battle.state.OnlineBattlePhase
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Phase router for the Battle tab — owns no state of its own beyond what it reads from
 * the injected [BattleSessionStore] singleton. Routing here is reactive: a
 * [LaunchedEffect] maps the session's phase/mode/onlinePhase to one of [BattleRoutes]
 * and navigates, always replacing the whole back stack (this flow has no push/pop
 * affordance of its own — resigning is explicit, via the top bar's Resign action).
 */
@Composable
fun BattleScreen(modifier: Modifier = Modifier) {
    val sessionStore: BattleSessionStore = koinInject()
    val session by sessionStore.state.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(session.phase, session.mode, session.onlinePhase) {
        navController.navigate(routeFor(session)) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = BattleHomeRoute, modifier = modifier) {
        composable<BattleHomeRoute> {
            BattleHomeScreen(viewModel = koinViewModel<BattleHomeViewModel>())
        }
        composable<OfflineBattleRoute> {
            OfflineBattleScreen(viewModel = koinViewModel<OfflineBattleViewModel>())
        }
        composable<BattleLobbyRoute> {
            BattleLobbyScreen(viewModel = koinViewModel<BattleLobbyViewModel>())
        }
        composable<OnlineBattleRoute> {
            OnlineBattleScreen(viewModel = koinViewModel<OnlineBattleViewModel>())
        }
        composable<BattleResultRoute> {
            // onClose only fires from BattleResultScreen's explicit "Pick another tech"
            // tap (not from disposal), so a plain reset is safe here — see
            // BattleResultScreen.kt for why that distinction matters.
            BattleResultScreen(viewModel = koinViewModel<BattleResultViewModel>(), onClose = sessionStore::reset)
        }
    }
}

private fun routeFor(session: BattleState): Any = when (session.phase) {
    BattlePhase.HOME -> BattleHomeRoute
    BattlePhase.RESULT -> BattleResultRoute
    BattlePhase.BATTLE -> when (session.mode) {
        BattleMode.OFFLINE -> OfflineBattleRoute
        BattleMode.ONLINE -> when (session.onlinePhase) {
            OnlineBattlePhase.LOBBY -> BattleLobbyRoute
            OnlineBattlePhase.MATCH -> OnlineBattleRoute
        }
    }
}
