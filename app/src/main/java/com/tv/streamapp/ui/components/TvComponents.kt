package com.tv.streamapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.tv.streamapp.ui.theme.TextPrimary
import com.tv.streamapp.ui.theme.TvBorder
import com.tv.streamapp.ui.theme.TvSurface

/** Parses "#RRGGBB" safely, falling back to [fallback]. */
fun parseColorOrDefault(hex: String?, fallback: Color = Color(0xFF00C8FF)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Makes an element focusable with the TV remote.
 * OK / Enter = click, holding OK = long click. Also works with touch.
 */
fun Modifier.tvClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {}
): Modifier = composed {
    val currentClick by rememberUpdatedState(onClick)
    val currentLong by rememberUpdatedState(onLongClick)
    val currentFocus by rememberUpdatedState(onFocusChange)
    var longFired by remember { mutableStateOf(false) }

    this
        .onFocusChanged { currentFocus(it.isFocused) }
        .onKeyEvent { event ->
            val isSelect = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter
            if (!isSelect) {
                false
            } else {
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        val repeat = event.nativeKeyEvent.repeatCount
                        if (repeat == 0) {
                            longFired = false
                        } else if (repeat == 1 && !longFired) {
                            val long = currentLong
                            if (long != null) {
                                longFired = true
                                long()
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (!longFired) currentClick()
                        longFired = false
                        true
                    }
                    else -> false
                }
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { currentClick() },
                onLongPress = { currentLong?.invoke() }
            )
        }
        .focusable()
}

/** A simple text button that lights up when focused. */
@Composable
fun TvButton(
    label: String,
    brand: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "btnScale")
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = label,
        color = if (focused) Color(0xFF0B0F19) else TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .scale(scale)
            .background(if (focused) brand else TvSurface, shape)
            .border(1.dp, if (focused) brand else TvBorder, shape)
            .tvClickable(onClick = onClick, onFocusChange = { focused = it })
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}
