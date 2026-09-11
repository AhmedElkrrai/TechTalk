package com.elkrrai.techtalk.navigation

import kotlinx.serialization.Serializable

/**
 * Top-level type-safe navigation destinations for [com.elkrrai.techtalk.AppContent].
 * [FeedRoute] and [BattleRoute] are the two — and only two — bottom-nav tabs `NavHost` owns,
 * navigated to with the standard save/restoreState + singleTop pattern so switching tabs never
 * piles up the back stack. The drawer and the Profile/Technologies/GetMoreContent screens are
 * not `NavHost` destinations: they render as overlays on top of the persistent Scaffold, driven
 * by `AppContentInner`'s private `Overlay` enum state instead.
 */
@Serializable
data object FeedRoute

@Serializable
data object BattleRoute
