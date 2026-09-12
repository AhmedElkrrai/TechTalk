package com.elkrrai.techtalk.presentation.battle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.elkrrai.techtalk.presentation.battle.home.BattleHomeScreen
import com.elkrrai.techtalk.presentation.battle.home.BattleHomeViewModel
import com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleScreen
import com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleViewModel
import com.elkrrai.techtalk.presentation.battle.online.BattleLobbyScreen
import com.elkrrai.techtalk.presentation.battle.online.BattleLobbyViewModel
import com.elkrrai.techtalk.presentation.battle.online.OnlineBattleScreen
import com.elkrrai.techtalk.presentation.battle.online.OnlineBattleViewModel
import com.elkrrai.techtalk.presentation.battle.result.BattleResultScreen
import com.elkrrai.techtalk.presentation.battle.result.BattleResultViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Owns the battle flow's nested nav graph. Each destination's ViewModel carries what it
 * needs forward as typed route arguments (technology/mode/time control/difficulty/player
 * identity, final score/XP) instead of a shared mutable session object, and navigates
 * itself by emitting a one-shot nav event that this composable collects and turns into a
 * `navController.navigate(...)` call — there's no reactive session-watching effect here
 * anymore. The battle flow has no back-stack affordance of its own (resigning is
 * explicit, via the top bar's Resign action), so every navigation here fully replaces
 * the stack rather than pushing.
 */
@Composable
fun BattleScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = BattleHomeRoute, modifier = modifier) {
        composable<BattleHomeRoute> {
            val viewModel = koinViewModel<BattleHomeViewModel>()
            LaunchedEffect(Unit) {
                viewModel.navigateToOfflineBattle.collect { navController.replaceStackWith(it) }
            }
            LaunchedEffect(Unit) {
                viewModel.navigateToLobby.collect { navController.replaceStackWith(it) }
            }
            BattleHomeScreen(viewModel = viewModel)
        }
        composable<OfflineBattleRoute> { backStackEntry ->
            val route: OfflineBattleRoute = backStackEntry.toRoute()
            val viewModel = koinViewModel<OfflineBattleViewModel> { parametersOf(route) }
            LaunchedEffect(Unit) {
                viewModel.navigateToResult.collect { navController.replaceStackWith(it) }
            }
            OfflineBattleScreen(viewModel = viewModel)
        }
        composable<BattleLobbyRoute> { backStackEntry ->
            val route: BattleLobbyRoute = backStackEntry.toRoute()
            val viewModel = koinViewModel<BattleLobbyViewModel> { parametersOf(route) }
            LaunchedEffect(Unit) {
                viewModel.navigateToMatch.collect { navController.replaceStackWith(it) }
            }
            BattleLobbyScreen(viewModel = viewModel)
        }
        composable<OnlineBattleRoute> { backStackEntry ->
            val route: OnlineBattleRoute = backStackEntry.toRoute()
            val viewModel = koinViewModel<OnlineBattleViewModel> { parametersOf(route) }
            LaunchedEffect(Unit) {
                viewModel.navigateToResult.collect { navController.replaceStackWith(it) }
            }
            OnlineBattleScreen(viewModel = viewModel)
        }
        composable<BattleResultRoute> { backStackEntry ->
            val route: BattleResultRoute = backStackEntry.toRoute()
            val viewModel = koinViewModel<BattleResultViewModel> { parametersOf(route) }
            LaunchedEffect(Unit) {
                viewModel.navigateToOfflineBattle.collect { navController.replaceStackWith(it) }
            }
            LaunchedEffect(Unit) {
                viewModel.navigateToHome.collect { navController.replaceStackWith(BattleHomeRoute) }
            }
            BattleResultScreen(viewModel = viewModel)
        }
    }
}

/** Every transition in this flow fully replaces the back stack — there's no push/pop
 * affordance here (see [BattleScreen]'s doc comment) — so every nav-event collector in
 * this file funnels through this one helper instead of repeating the same
 * `navigate(route) { popUpTo(...) { inclusive = true }; launchSingleTop = true }` call. */
private fun NavController.replaceStackWith(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
