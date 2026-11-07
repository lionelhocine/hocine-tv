package com.tv.streamapp.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tv.streamapp.Config
import com.tv.streamapp.data.model.Channel
import com.tv.streamapp.data.preferences.UserPreferences
import com.tv.streamapp.data.repository.DataSource
import com.tv.streamapp.data.repository.PlaylistRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val KEY_ALL = "__all__"
const val KEY_FAVORITES = "__favorites__"

sealed interface TvNavigationScreen {
    data object Splash : TvNavigationScreen
    data object Home : TvNavigationScreen
    data class Player(val channel: Channel) : TvNavigationScreen
}

data class TvUiState(
    val currentScreen: TvNavigationScreen = TvNavigationScreen.Splash,
    val appName: String = Config.APP_NAME,
    val logoUrl: String? = null,
    val backgroundUrl: String? = null,
    val primaryColorHex: String = Config.DEFAULT_PRIMARY_COLOR,
    val announcement: String? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val isOffline: Boolean = false,
    val categories: List<String> = listOf(KEY_ALL, KEY_FAVORITES),
    val selectedCategory: String = KEY_ALL,
    val allChannels: List<Channel> = emptyList(),
    val displayedChannels: List<Channel> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isQuickDrawerOpen: Boolean = false,
    val autoResume: Boolean = false
)

private fun TvUiState.recomputed(): TvUiState {
    val query = searchQuery.trim()
    val base: List<Channel> = if (query.isNotEmpty()) {
        allChannels
    } else if (selectedCategory == KEY_ALL) {
        allChannels
    } else if (selectedCategory == KEY_FAVORITES) {
        allChannels.filter { it.id in favoriteIds }
    } else {
        allChannels.filter { it.category == selectedCategory }
    }
    val shown = if (query.isEmpty()) base else base.filter { it.name.contains(query, ignoreCase = true) }
    return copy(displayedChannels = shown)
}

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(application)
    private val prefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(TvUiState())
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    private var lastChannelId: String? = null
    private var initialLoadDone = false

    init {
        viewModelScope.launch {
            prefs.favorites.collect { favs ->
                _uiState.update { it.copy(favoriteIds = favs).recomputed() }
            }
        }
        viewModelScope.launch {
            prefs.autoResume.collect { value -> _uiState.update { it.copy(autoResume = value) } }
        }
        viewModelScope.launch {
            prefs.lastChannelId.collect { value -> lastChannelId = value }
        }
        loadPlaylist(isRefresh = false)
    }

    fun loadPlaylist(isRefresh: Boolean) {
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            val result = repository.loadPlaylist(forceRemote = isRefresh)

            if (!initialLoadDone) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                if (elapsed < 1500L) delay(1500L - elapsed)
            }

            result.onSuccess { data ->
                val channels = data.categories.flatMap { group ->
                    group.channels.map { raw -> Channel.fromRaw(raw, group.name) }
                }
                val names = data.categories
                    .filter { it.channels.isNotEmpty() }
                    .map { it.name.trim() }
                    .distinct()
                val branding = data.branding

                _uiState.update { old ->
                    val keys = listOf(KEY_ALL, KEY_FAVORITES) + names
                    val selected = if (old.selectedCategory in keys) old.selectedCategory else KEY_ALL
                    val screen = decideScreen(old, channels)
                    old.copy(
                        currentScreen = screen,
                        appName = branding?.appName?.takeIf { it.isNotBlank() } ?: Config.APP_NAME,
                        logoUrl = branding?.logoUrl?.takeIf { it.isNotBlank() },
                        backgroundUrl = branding?.backgroundUrl?.takeIf { it.isNotBlank() },
                        primaryColorHex = branding?.primaryColor?.takeIf { it.isNotBlank() }
                            ?: Config.DEFAULT_PRIMARY_COLOR,
                        announcement = data.announcement?.takeIf { it.isNotBlank() },
                        isLoading = false,
                        hasError = false,
                        isOffline = data.source != DataSource.REMOTE,
                        categories = keys,
                        selectedCategory = selected,
                        allChannels = channels
                    ).recomputed()
                }
                initialLoadDone = true
            }.onFailure {
                _uiState.update { old ->
                    old.copy(
                        currentScreen = if (old.currentScreen is TvNavigationScreen.Splash) {
                            TvNavigationScreen.Home
                        } else {
                            old.currentScreen
                        },
                        isLoading = false,
                        hasError = true
                    )
                }
                initialLoadDone = true
            }
        }
    }

    private fun decideScreen(old: TvUiState, channels: List<Channel>): TvNavigationScreen {
        if (old.currentScreen !is TvNavigationScreen.Splash) return old.currentScreen
        if (old.autoResume) {
            val last = channels.firstOrNull { it.id == lastChannelId }
            if (last != null) return TvNavigationScreen.Player(last)
        }
        return TvNavigationScreen.Home
    }

    fun selectCategory(key: String) {
        _uiState.update { it.copy(selectedCategory = key).recomputed() }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query).recomputed() }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update {
            if (open) it.copy(isSearchOpen = true)
            else it.copy(isSearchOpen = false, searchQuery = "").recomputed()
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch { prefs.toggleFavorite(channelId) }
    }

    fun toggleAutoResume() {
        val newValue = !_uiState.value.autoResume
        viewModelScope.launch { prefs.setAutoResume(newValue) }
    }

    fun openPlayer(channel: Channel) {
        _uiState.update {
            it.copy(currentScreen = TvNavigationScreen.Player(channel), isQuickDrawerOpen = false)
        }
        viewModelScope.launch { prefs.setLastChannel(channel.id) }
    }

    fun returnToHome() {
        _uiState.update { it.copy(currentScreen = TvNavigationScreen.Home, isQuickDrawerOpen = false) }
    }

    fun setQuickDrawerOpen(open: Boolean) {
        _uiState.update { it.copy(isQuickDrawerOpen = open) }
    }

    fun playNextChannel() = stepChannel(1)

    fun playPreviousChannel() = stepChannel(-1)

    private fun stepChannel(delta: Int) {
        val state = _uiState.value
        val current = (state.currentScreen as? TvNavigationScreen.Player)?.channel ?: return
        val list = state.displayedChannels.ifEmpty { state.allChannels }
        if (list.size < 2) return
        val index = list.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val next = list[(index + delta + list.size) % list.size]
        openPlayer(next)
    }
}
