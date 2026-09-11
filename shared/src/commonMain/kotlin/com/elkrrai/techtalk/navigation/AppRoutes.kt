package com.elkrrai.techtalk.navigation

import kotlinx.serialization.Serializable

/**
 * Top-level type-safe navigation destinations for [com.elkrrai.techtalk.AppContent].
 * [FeedRoute] and [BattleRoute] are the two bottom-nav tabs (navigated to with the
 * standard save/restoreState + singleTop pattern so switching tabs never piles up the
 * back stack); the rest are full-screen overlays pushed from the drawer.
 */
@Serializable
data object FeedRoute

@Serializable
data object BattleRoute

@Serializable
data object UserProfileRoute

@Serializable
data object TechnologiesRoute

@Serializable
data object GetMoreContentRoute
