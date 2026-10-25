package com.example.romilauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import com.example.romilauncher.ui.theme.Romi

/**
 * The one focusable control every Romi screen is built from. Focus shows as an outline, a lighter fill
 * and a small scale-up, so it never depends on colour alone.
 */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    container: Color = Romi.colors.card,
    focusedContainer: Color = Romi.colors.cardFocused,
    contentColor: Color = Romi.colors.textPrimary,
    focusedScale: Float? = null,
    enabled: Boolean = true,
    description: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Romi.colors
    val motion = Romi.motion
    val scale = if (motion.animated || motion.focusScale > 1f) (focusedScale ?: motion.focusScale) else 1f
    val ring = if (motion.strongRing) 3.dp else 2.dp
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    // Focus that arrives from code (restored after a dialog, say) doesn't scroll on its own, and the
    // list may still shift after it lands, so ask again whenever the focused control moves.
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(focused) {
        if (focused) {
            withFrameNanos { }
            bringIntoView.bringIntoView()
        }
    }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        modifier = modifier
            .bringIntoViewRequester(bringIntoView)
            .onGloballyPositioned { if (focused) scope.launch { bringIntoView.bringIntoView() } }
            .semantics {
            role = Role.Button
            if (description != null) contentDescription = description
        },
        shape = ClickableSurfaceDefaults.shape(shape = shape, focusedShape = shape, pressedShape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = contentColor,
            focusedContainerColor = focusedContainer,
            focusedContentColor = colors.textPrimary,
            pressedContainerColor = focusedContainer,
            pressedContentColor = colors.textPrimary,
            disabledContainerColor = container.copy(alpha = container.alpha * 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f),
        ),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = scale, pressedScale = scale * 0.98f),
        interactionSource = interactionSource,
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = if (motion.glow) Glow(colors.accent.copy(alpha = 0.35f), 14.dp) else Glow.None,
        ),
    ) {
        content()
        if (focused) {
            // Two-tone ring: a light line with a dark line just inside it, so it reads on white
            // banners as well as on dark cards.
            Box(
                Modifier
                    .matchParentSize()
                    .border(BorderStroke(ring, colors.focusRing), shape)
                    .padding(ring)
                    .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.5f)), shape),
            )
        }
    }
}

/** Requests focus once, after the first composition. Used to land focus inside dialogs and screens. */
@Composable
fun Modifier.autoFocus(enabled: Boolean = true): Modifier {
    val requester = remember { FocusRequester() }
    if (enabled) {
        LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
    }
    return focusRequester(requester)
}

/**
 * Remembers which home-screen element had focus so it can be restored after a sub-screen closes.
 * Lives in the view model, so it also survives configuration changes.
 */
class FocusMemory {
    var last: String? = null
    var pending: String? = null
}

val LocalFocusMemory = androidx.compose.runtime.staticCompositionLocalOf { FocusMemory() }

/**
 * Tracks [key] as the last focused home element, and takes focus itself when [key] is the pending
 * restore target. An optional [requester] lets callers aim default focus at the same element.
 */
@Composable
fun Modifier.homeFocus(key: String, requester: FocusRequester? = null): Modifier {
    val memory = LocalFocusMemory.current
    val own = requester ?: remember { FocusRequester() }
    LaunchedEffect(key) {
        if (memory.pending == key) {
            memory.pending = null
            runCatching { own.requestFocus() }
        }
    }
    return focusRequester(own).onFocusChanged { if (it.hasFocus) memory.last = key }
}
