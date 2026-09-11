package com.elkrrai.techtalk.presentation.battle

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations for [BattleScreen]'s nested nav graph. These are driven
 * reactively FROM [com.elkrrai.techtalk.presentation.battle.state.BattleSessionStore]
 * state (a [LaunchedEffect] maps the current phase/mode to one of these and navigates),
 * not by direct user taps — the battle flow has no back-stack affordance of its own
 * (resigning is explicit, via the top bar), so navigation here always fully replaces
 * the stack rather than pushing.
 */
@Serializable
data object BattleHomeRoute

@Serializable
data object OfflineBattleRoute

@Serializable
data object BattleLobbyRoute

@Serializable
data object OnlineBattleRoute

@Serializable
data object BattleResultRoute
