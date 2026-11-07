package com.tv.streamapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tv.streamapp.ui.screens.HomeScreen
import com.tv.streamapp.ui.screens.PlayerScreen
import com.tv.streamapp.ui.screens.SplashScreen
import com.tv.streamapp.ui.theme.AndroidTvStreamTheme
import com.tv.streamapp.ui.viewmodel.TvNavigationScreen
import com.tv.streamapp.ui.viewmodel.TvViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidTvStreamTheme {
                val uiState by viewModel.uiState.collectAsState()

                when (val screen = uiState.currentScreen) {
                    is TvNavigationScreen.Splash -> {
                        SplashScreen(
                            appName = uiState.appName,
                            logoUrl = uiState.logoUrl,
                            primaryColorHex = uiState.primaryColorHex,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is TvNavigationScreen.Home -> {
                        BackHandler(enabled = uiState.isSearchOpen) {
                            viewModel.setSearchOpen(false)
                        }
                        HomeScreen(
                            uiState = uiState,
                            onSelectCategory = { key -> viewModel.selectCategory(key) },
                            onPlayChannel = { channel -> viewModel.openPlayer(channel) },
                            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                            onRefresh = { viewModel.loadPlaylist(isRefresh = true) },
                            onSearchQueryChange = { q -> viewModel.updateSearchQuery(q) },
                            onToggleSearch = { open -> viewModel.setSearchOpen(open) },
                            onToggleAutoResume = { viewModel.toggleAutoResume() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is TvNavigationScreen.Player -> {
                        BackHandler {
                            if (uiState.isQuickDrawerOpen) {
                                viewModel.setQuickDrawerOpen(false)
                            } else {
                                viewModel.returnToHome()
                            }
                        }
                        PlayerScreen(
                            channel = screen.channel,
                            channelsList = uiState.displayedChannels.ifEmpty { uiState.allChannels },
                            isDrawerOpen = uiState.isQuickDrawerOpen,
                            onDrawerChange = { open -> viewModel.setQuickDrawerOpen(open) },
                            onNextChannel = { viewModel.playNextChannel() },
                            onPreviousChannel = { viewModel.playPreviousChannel() },
                            onSelectChannel = { ch -> viewModel.openPlayer(ch) },
                            primaryColorHex = uiState.primaryColorHex,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
