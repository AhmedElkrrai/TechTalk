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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private enum class AppTab { Feed, Battle }
private enum class AppScreen { Main, UserProfile, Technologies, GetMoreContent }

@Composable
fun AppContent() {
    TechTalkTheme { AppContentInner() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContentInner(
    feedViewModel: FeedViewModel = koinViewModel(),
    technologyListViewModel: TechnologyListViewModel = koinViewModel(),
    getMoreContentViewModel: GetMoreContentViewModel = koinViewModel(),
    userProfileViewModel: UserProfileViewModel = koinViewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Feed) }
    var currentScreen by remember { mutableStateOf(AppScreen.Main) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    PlatformBackHandler(enabled = isDrawerOpen || currentScreen != AppScreen.Main) {
        if (isDrawerOpen) {
            isDrawerOpen = false
        } else {
            currentScreen = AppScreen.Main
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.Main -> {
                val profileState by userProfileViewModel.state.collectAsState()
                val feedState by feedViewModel.state.collectAsState()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    if (selectedTab == AppTab.Feed && feedState.showTipsCounter) {
                                        "TechTalk  ${feedState.currentPage + 1} / ${feedState.filteredTips.size}"
                                    } else {
                                        "TechTalk"
                                    }
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { isDrawerOpen = true }) { Text("☰") }
                            },
                            actions = {
                                if (selectedTab == AppTab.Feed) {
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
                                selected = selectedTab == AppTab.Feed,
                                onClick = { selectedTab = AppTab.Feed },
                                icon = { Text("📱") },
                                label = { Text("Feed") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.Battle,
                                onClick = { selectedTab = AppTab.Battle },
                                icon = { Text("⚔️") },
                                label = { Text("Battle") }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        when (selectedTab) {
                            AppTab.Feed -> FeedScreen(
                                viewModel = feedViewModel,
                                onBrowseTechnologies = { currentScreen = AppScreen.Technologies },
                                modifier = Modifier.fillMaxSize()
                            )
                            AppTab.Battle -> BattleScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isDrawerOpen,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isDrawerOpen = false }
                            )
                    )
                }

                AnimatedVisibility(
                    visible = isDrawerOpen,
                    enter = slideInHorizontally(tween(220)) { -it },
                    exit = slideOutHorizontally(tween(220)) { -it }
                ) {
                    MenuDrawer(
                        userName = profileState.name,
                        userAvatarKey = profileState.selectedAvatarKey,
                        onProfileClick = {
                            isDrawerOpen = false
                            currentScreen = AppScreen.UserProfile
                        },
                        onTechnologiesClick = {
                            isDrawerOpen = false
                            currentScreen = AppScreen.Technologies
                        },
                        onGetMoreContentClick = {
                            isDrawerOpen = false
                            currentScreen = AppScreen.GetMoreContent
                        }
                    )
                }
            }

            AppScreen.UserProfile -> UserProfileScreen(
                viewModel = userProfileViewModel,
                onClose = { currentScreen = AppScreen.Main },
                modifier = Modifier.fillMaxSize()
            )

            AppScreen.Technologies -> TechnologyListScreen(
                viewModel = technologyListViewModel,
                onClose = { currentScreen = AppScreen.Main },
                modifier = Modifier.fillMaxSize()
            )

            AppScreen.GetMoreContent -> GetMoreContentScreen(
                viewModel = getMoreContentViewModel,
                onClose = { currentScreen = AppScreen.Main },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
