package com.elkrrai.techtalk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elkrrai.techtalk.navigation.BattleRoute
import com.elkrrai.techtalk.navigation.FeedRoute
import com.elkrrai.techtalk.presentation.battle.BattleScreen
import com.elkrrai.techtalk.presentation.component.PlatformBackHandler
import com.elkrrai.techtalk.presentation.feed.FeedViewModel
import com.elkrrai.techtalk.presentation.feed.ui.FeedScreen
import com.elkrrai.techtalk.presentation.getmorecontent.GetMoreContentViewModel
import com.elkrrai.techtalk.presentation.getmorecontent.ui.GetMoreContentScreen
import com.elkrrai.techtalk.presentation.menu.ui.MenuDrawer
import com.elkrrai.techtalk.presentation.technologylist.TechnologyListViewModel
import com.elkrrai.techtalk.presentation.technologylist.ui.TechnologyListScreen
import com.elkrrai.techtalk.presentation.theme.TechTalkTheme
import com.elkrrai.techtalk.presentation.userprofile.UserProfileViewModel
import com.elkrrai.techtalk.presentation.userprofile.ui.UserProfileScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppContent() {
    TechTalkTheme { AppContentInner() }
}

/**
 * Everything that can appear as a full-screen (or scrim) layer on top of the persistent
 * Feed/Battle chrome — the hand-rolled nav drawer plus the three overlay screens it opens.
 * A single [Overlay]? keeps these mutually exclusive by construction: only one can be
 * visible at a time, switching between them is one atomic assignment, and there is one
 * back-press target regardless of which layer is showing. They render on top of (not
 * inside) the tab [NavHost] so the outer Scaffold's chrome never has to change when one
 * opens/closes — that decoupling is what fixes the "new chrome + old content" frame bug.
 */
private enum class Overlay { Drawer, Profile, Technologies, GetMoreContent }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContentInner(
    feedViewModel: FeedViewModel = koinViewModel(),
    technologyListViewModel: TechnologyListViewModel = koinViewModel(),
    getMoreContentViewModel: GetMoreContentViewModel = koinViewModel(),
    userProfileViewModel: UserProfileViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isBattleSelected = backStackEntry?.destination?.hasRoute<BattleRoute>() == true

    var overlay by remember { mutableStateOf<Overlay?>(null) }

    PlatformBackHandler(enabled = overlay != null) { overlay = null }

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo<FeedRoute> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val feedState by feedViewModel.state.collectAsState()
    val profileState by userProfileViewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (!isBattleSelected && feedState.showTipsCounter) {
                                "TechTalk  ${feedState.currentPage + 1} / ${feedState.filteredTips.size}"
                            } else {
                                "TechTalk"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { overlay = Overlay.Drawer },
                            modifier = Modifier.semantics { contentDescription = "Menu" }
                        ) { Text("☰") }
                    },
                    actions = {
                        if (!isBattleSelected) {
                            IconButton(onClick = feedViewModel::onOpenFilterSheet) {
                                Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = !isBattleSelected,
                        onClick = { navigateToTab(FeedRoute) },
                        icon = { Text("📱") },
                        label = { Text("Feed") }
                    )
                    NavigationBarItem(
                        selected = isBattleSelected,
                        onClick = { navigateToTab(BattleRoute) },
                        icon = { Text("⚔️") },
                        label = { Text("Battle") }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = FeedRoute,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                composable<FeedRoute> {
                    FeedScreen(
                        viewModel = feedViewModel,
                        onBrowseTechnologies = { overlay = Overlay.Technologies },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<BattleRoute> {
                    BattleScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }

        FadeOverlay(visible = overlay == Overlay.Profile) {
            UserProfileScreen(
                viewModel = userProfileViewModel,
                onClose = { overlay = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        FadeOverlay(visible = overlay == Overlay.Technologies) {
            TechnologyListScreen(
                viewModel = technologyListViewModel,
                onClose = { overlay = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        FadeOverlay(visible = overlay == Overlay.GetMoreContent) {
            GetMoreContentScreen(
                viewModel = getMoreContentViewModel,
                onClose = { overlay = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        FadeOverlay(visible = overlay == Overlay.Drawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { overlay = null }
                    )
            )
        }

        AnimatedVisibility(
            visible = overlay == Overlay.Drawer,
            enter = slideInHorizontally(tween(220)) { -it },
            exit = slideOutHorizontally(tween(220)) { -it }
        ) {
            MenuDrawer(
                userName = profileState.name,
                userAvatarKey = profileState.selectedAvatarKey,
                onProfileClick = { overlay = Overlay.Profile },
                onTechnologiesClick = { overlay = Overlay.Technologies },
                onGetMoreContentClick = { overlay = Overlay.GetMoreContent }
            )
        }
    }
}

@Composable
private fun FadeOverlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        content()
    }
}
