package com.tv.streamapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tv.streamapp.R
import com.tv.streamapp.data.model.Channel
import com.tv.streamapp.ui.components.TvButton
import com.tv.streamapp.ui.components.parseColorOrDefault
import com.tv.streamapp.ui.components.tvClickable
import com.tv.streamapp.ui.theme.TextPrimary
import com.tv.streamapp.ui.theme.TextSecondary
import com.tv.streamapp.ui.theme.TvBackground
import com.tv.streamapp.ui.theme.TvBorder
import com.tv.streamapp.ui.theme.TvSurface
import com.tv.streamapp.ui.theme.TvSurfaceFocused
import com.tv.streamapp.ui.viewmodel.KEY_ALL
import com.tv.streamapp.ui.viewmodel.KEY_FAVORITES
import com.tv.streamapp.ui.viewmodel.TvUiState

@Composable
private fun categoryLabel(key: String): String = when (key) {
    KEY_ALL -> stringResource(R.string.category_all)
    KEY_FAVORITES -> stringResource(R.string.category_favorites)
    else -> key
}

@Composable
fun HomeScreen(
    uiState: TvUiState,
    onSelectCategory: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onToggleAutoResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = parseColorOrDefault(uiState.primaryColorHex)
    val firstCardFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(uiState.allChannels.isNotEmpty()) {
        if (uiState.allChannels.isNotEmpty()) {
            kotlinx.coroutines.delay(200)
            runCatching { firstCardFocus.requestFocus() }
        }
    }
    LaunchedEffect(uiState.isSearchOpen) {
        if (uiState.isSearchOpen) {
            kotlinx.coroutines.delay(100)
            runCatching { searchFocus.requestFocus() }
        }
    }

    Box(modifier = modifier.background(TvBackground)) {
        if (!uiState.backgroundUrl.isNullOrBlank()) {
            AsyncImage(
                model = uiState.backgroundUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.25f,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // ---------- Left: logo + categories ----------
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(Color(0xCC080A1C))
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.hocine_logo_white),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = uiState.appName,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(20.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.categories) { key ->
                        CategoryItem(
                            label = categoryLabel(key),
                            selected = key == uiState.selectedCategory && uiState.searchQuery.isBlank(),
                            brand = brand,
                            onClick = { onSelectCategory(key) }
                        )
                    }
                }
            }

            // ---------- Right: top bar + grid ----------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isSearchOpen) {
                        SearchField(
                            query = uiState.searchQuery,
                            onQueryChange = onSearchQueryChange,
                            brand = brand,
                            focusRequester = searchFocus,
                            modifier = Modifier.weight(1f)
                        )
                        TvButton(
                            label = stringResource(R.string.action_close_search),
                            brand = brand,
                            onClick = { onToggleSearch(false) }
                        )
                    } else {
                        Text(
                            text = categoryLabel(uiState.selectedCategory),
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TvButton(
                            label = stringResource(R.string.action_search),
                            brand = brand,
                            onClick = { onToggleSearch(true) }
                        )
                        TvButton(
                            label = stringResource(R.string.refresh_playlist),
                            brand = brand,
                            onClick = onRefresh
                        )
                        TvButton(
                            label = stringResource(
                                if (uiState.autoResume) R.string.resume_on else R.string.resume_off
                            ),
                            brand = brand,
                            onClick = onToggleAutoResume
                        )
                    }
                }

                val banner = uiState.announcement
                if (!banner.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = banner,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, brand, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                if (uiState.isOffline && !uiState.isLoading) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.offline_mode),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                val channels = uiState.displayedChannels
                if (channels.isEmpty()) {
                    val message = when {
                        uiState.isLoading -> stringResource(R.string.loading_playlist)
                        uiState.hasError && uiState.allChannels.isEmpty() -> stringResource(R.string.load_error)
                        uiState.selectedCategory == KEY_FAVORITES && uiState.searchQuery.isBlank() ->
                            stringResource(R.string.no_favorites)
                        else -> stringResource(R.string.no_channels)
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = message, color = TextSecondary, fontSize = 20.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(190.dp),
                        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(channels) { index, channel ->
                            ChannelCard(
                                channel = channel,
                                isFavorite = channel.id in uiState.favoriteIds,
                                brand = brand,
                                onClick = { onPlayChannel(channel) },
                                onLongClick = { onToggleFavorite(channel.id) },
                                modifier = if (index == 0) Modifier.focusRequester(firstCardFocus) else Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    label: String,
    selected: Boolean,
    brand: Color,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = label,
        color = if (focused) Color(0xFF0B0F19) else if (selected) brand else TextPrimary,
        fontSize = 18.sp,
        fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (focused) brand else if (selected) TvSurfaceFocused else Color.Transparent, shape)
            .tvClickable(onClick = onClick, onFocusChange = { focused = it })
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    brand: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "cardScale")
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .scale(scale)
            .background(if (focused) TvSurfaceFocused else TvSurface, shape)
            .border(if (focused) 3.dp else 1.dp, if (focused) brand else TvBorder, shape)
            .tvClickable(onClick = onClick, onLongClick = onLongClick, onFocusChange = { focused = it })
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(TvBackground),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                placeholder = painterResource(R.drawable.channel_placeholder),
                error = painterResource(R.drawable.channel_placeholder),
                fallback = painterResource(R.drawable.channel_placeholder),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
            if (isFavorite) {
                Text(
                    text = "\u2605",
                    color = Color(0xFFF59E0B),
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = channel.name,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    brand: Color,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .background(TvSurface, shape)
            .border(2.dp, brand, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.search_hint),
                color = TextSecondary,
                fontSize = 18.sp
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 18.sp),
            cursorBrush = SolidColor(brand),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    }
}
