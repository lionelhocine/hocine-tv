package com.tv.streamapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tv.streamapp.Config
import com.tv.streamapp.R
import com.tv.streamapp.data.model.Channel
import com.tv.streamapp.ui.components.parseColorOrDefault
import com.tv.streamapp.ui.components.tvClickable
import com.tv.streamapp.ui.theme.TextPrimary
import com.tv.streamapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    channel: Channel,
    channelsList: List<Channel>,
    isDrawerOpen: Boolean,
    onDrawerChange: (Boolean) -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onSelectChannel: (Channel) -> Unit,
    primaryColorHex: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val brand = parseColorOrDefault(primaryColorHex)

    val latestNext by rememberUpdatedState(onNextChannel)
    val latestPrevious by rememberUpdatedState(onPreviousChannel)

    var overlayVisible by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var retryShown by remember(channel.id) { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }

    val exo = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(Config.PLAYER_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(http))
            .build()
            .apply { playWhenReady = true }
    }

    // Keep the TV awake while the player is on screen, and free the player when leaving.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            exo.release()
        }
    }

    // Load the channel, retry on errors, then skip to the next channel after too many failures.
    DisposableEffect(channel.id) {
        var errors = 0
        var disposed = false
        val loadChannel = {
            exo.setMediaItem(MediaItem.fromUri(channel.url))
            exo.prepare()
            exo.playWhenReady = true
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errors = 0
                    retryShown = 0
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errors += 1
                retryShown = errors
                scope.launch {
                    if (errors <= Config.MAX_STREAM_RETRY_COUNT) {
                        delay(2000)
                        if (!disposed) loadChannel()
                    } else {
                        delay(1500)
                        if (!disposed) latestNext()
                    }
                }
            }
        }
        exo.addListener(listener)
        loadChannel()
        onDispose {
            disposed = true
            exo.removeListener(listener)
        }
    }

    LaunchedEffect(channel.id) { overlayVisible = true }
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            delay(4000)
            overlayVisible = false
        }
    }
    LaunchedEffect(isDrawerOpen) {
        if (!isDrawerOpen) {
            delay(50)
            runCatching { rootFocus.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .focusRequester(rootFocus)
            .onKeyEvent { event ->
                if (isDrawerOpen || event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionUp, Key.ChannelUp -> { latestNext(); true }
                        Key.DirectionDown, Key.ChannelDown -> { latestPrevious(); true }
                        Key.DirectionLeft -> { onDrawerChange(true); true }
                        Key.DirectionRight -> { overlayVisible = true; true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            overlayVisible = !overlayVisible
                            true
                        }
                        else -> false
                    }
                }
            }
            .focusable()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    isFocusable = false
                    isFocusableInTouchMode = false
                    player = exo
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Retry / skip message
        if (retryShown > 0) {
            val text = if (retryShown <= Config.MAX_STREAM_RETRY_COUNT) {
                stringResource(R.string.stream_error, retryShown)
            } else {
                stringResource(R.string.stream_skipping)
            }
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )
        } else if (isBuffering) {
            Text(
                text = "...",
                color = TextPrimary,
                fontSize = 40.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Channel info overlay (OK button)
        if (overlayVisible && !isDrawerOpen) {
            val index = channelsList.indexOfFirst { it.id == channel.id }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                    .padding(horizontal = 40.dp, vertical = 28.dp)
            ) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.channel_placeholder),
                    error = painterResource(R.drawable.channel_placeholder),
                    fallback = painterResource(R.drawable.channel_placeholder),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel.category,
                        color = TextSecondary,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index >= 0) {
                    Text(
                        text = "${index + 1} / ${channelsList.size}",
                        color = brand,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Quick channel list (LEFT button)
        if (isDrawerOpen) {
            QuickDrawer(
                channels = channelsList,
                current = channel,
                brand = brand,
                onSelect = { picked ->
                    onSelectChannel(picked)
                    onDrawerChange(false)
                },
                onClose = { onDrawerChange(false) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun QuickDrawer(
    channels: List<Channel>,
    current: Channel,
    brand: Color,
    onSelect: (Channel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentFocus = remember { FocusRequester() }
    val currentIndex = channels.indexOfFirst { it.id == current.id }.coerceAtLeast(0)

    LaunchedEffect(Unit) {
        listState.scrollToItem(currentIndex)
        delay(120)
        runCatching { currentFocus.requestFocus() }
    }

    Column(
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(Color(0xEB080A1C))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onClose()
                    true
                } else {
                    false
                }
            }
            .padding(16.dp)
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(channels) { index, channel ->
                DrawerRow(
                    channel = channel,
                    isCurrent = channel.id == current.id,
                    brand = brand,
                    onClick = { onSelect(channel) },
                    modifier = if (index == currentIndex) Modifier.focusRequester(currentFocus) else Modifier
                )
            }
        }
    }
}

@Composable
private fun DrawerRow(
    channel: Channel,
    isCurrent: Boolean,
    brand: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (focused) brand else Color(0x33FFFFFF), shape)
            .then(if (isCurrent) Modifier.border(2.dp, brand, shape) else Modifier)
            .tvClickable(onClick = onClick, onFocusChange = { focused = it })
            .padding(10.dp)
    ) {
        AsyncImage(
            model = channel.logo,
            contentDescription = null,
            placeholder = painterResource(R.drawable.channel_placeholder),
            error = painterResource(R.drawable.channel_placeholder),
            fallback = painterResource(R.drawable.channel_placeholder),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = channel.name,
            color = if (focused) Color(0xFF0B0F19) else TextPrimary,
            fontSize = 18.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
