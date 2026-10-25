package com.example.romilauncher.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.romilauncher.data.SidebarMode
import com.example.romilauncher.ui.Route
import com.example.romilauncher.ui.ShortcutUi
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing
import com.example.romilauncher.ui.theme.RomiType

@Composable
fun Sidebar(
    route: Route,
    mode: SidebarMode,
    shortcuts: List<ShortcutUi>,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onDisplayMode: () -> Unit,
    onTimer: () -> Unit,
    onShortcut: (ShortcutUi) -> Unit,
    onShortcutMenu: (ShortcutUi) -> Unit,
    onAddShortcut: () -> Unit,
    modifier: Modifier = Modifier,
    homeRequester: FocusRequester? = null,
) {
    val colors = Romi.colors
    var hasFocus by remember { mutableStateOf(false) }
    val compact = mode == SidebarMode.COMPACT
    val expanded = !compact || hasFocus
    val width by animateDpAsState(
        targetValue = if (expanded) RomiSpacing.sidebarExpanded else RomiSpacing.sidebarCompact,
        animationSpec = tween(Romi.motion.focusMs.coerceAtLeast(1)),
        label = "sidebar-width",
    )
    // A compact sidebar floats over the content when it opens, so it needs its own backdrop.
    val backdrop = if (compact && expanded) colors.surface else Color.Transparent

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(backdrop)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusGroup()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RomiSpacing.s, vertical = RomiSpacing.s),
        verticalArrangement = Arrangement.spacedBy(RomiSpacing.xs),
    ) {
        SidebarItem(
            icon = RomiIcons.home, label = "Home", expanded = expanded,
            selected = route is Route.Home, onClick = onHome,
            modifier = Modifier.homeFocus("side:home", homeRequester),
        )
        SidebarItem(
            icon = RomiIcons.search, label = "Search", expanded = expanded,
            selected = route is Route.Search, onClick = onSearch,
            modifier = Modifier.homeFocus("side:search"),
        )
        SidebarItem(
            icon = RomiIcons.display, label = "Display Mode", expanded = expanded,
            selected = false, onClick = onDisplayMode,
            modifier = Modifier.homeFocus("side:display"),
        )
        SidebarItem(
            icon = RomiIcons.timer, label = "Off Timer", expanded = expanded,
            selected = false, onClick = onTimer,
            modifier = Modifier.homeFocus("side:timer"),
        )

        Box(
            Modifier.padding(vertical = RomiSpacing.s, horizontal = RomiSpacing.s)
                .fillMaxWidth().height(1.dp).background(colors.divider),
        )
        if (expanded) {
            Text(
                "Shortcuts",
                style = RomiType.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = RomiSpacing.m, bottom = RomiSpacing.xs),
            )
        }
        shortcuts.forEach { item ->
            ShortcutItem(
                item = item,
                expanded = expanded,
                onClick = { onShortcut(item) },
                onLongClick = { onShortcutMenu(item) },
                modifier = Modifier.homeFocus("side:shortcut:${item.shortcut.id}"),
            )
        }
        SidebarItem(
            icon = RomiIcons.add, label = "Add Shortcut", expanded = expanded,
            selected = false, onClick = onAddShortcut, small = true,
            modifier = Modifier.homeFocus("side:add"),
        )
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    expanded: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    small: Boolean = false,
) {
    val colors = Romi.colors
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        container = if (selected) colors.card else Color.Transparent,
        contentColor = if (selected) colors.textPrimary else colors.textSecondary,
        focusedScale = 1.03f,
        description = if (selected) "$label, current screen" else label,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (small) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (small) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(colors.card), contentAlignment = Alignment.Center) {
                    RomiIcon(icon, size = 18.dp, tint = Color.Unspecified)
                }
            } else {
                RomiIcon(icon, size = 24.dp, tint = Color.Unspecified)
            }
            if (expanded) {
                Spacer(Modifier.width(RomiSpacing.m - 2.dp))
                Text(
                    label,
                    style = if (small) RomiType.label else RomiType.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (trailing != null) {
                    Text(trailing, style = RomiType.label, color = colors.accent, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ShortcutItem(
    item: ShortcutUi,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Romi.colors
    val label = item.shortcut.label
    FocusSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(14.dp),
        container = Color.Transparent,
        contentColor = colors.textSecondary,
        focusedScale = 1.03f,
        description = if (item.available) label else "$label, unavailable",
        modifier = modifier.fillMaxWidth().alpha(if (item.available) 1f else 0.55f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShortcutGlyph(item, size = 28.dp)
            if (expanded) {
                Spacer(Modifier.width(RomiSpacing.m - 2.dp))
                Text(label, style = RomiType.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A shortcut's round icon: either the target app's own icon or a glyph from the library. */
@Composable
fun ShortcutGlyph(item: ShortcutUi, size: androidx.compose.ui.unit.Dp) {
    val colors = Romi.colors
    Box(Modifier.size(size).clip(CircleShape).background(colors.card), contentAlignment = Alignment.Center) {
        val app = item.app
        if (item.shortcut.iconKey == com.example.romilauncher.data.Shortcut.APP_ICON && app != null) {
            val image = rememberAppImage(app).value
            if (image != null) {
                Image(
                    bitmap = image.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size * 0.7f),
                )
            }
        } else {
            val key = if (item.shortcut.iconKey == com.example.romilauncher.data.Shortcut.APP_ICON) "star" else item.shortcut.iconKey
            RomiIcon(RomiIcons.get(key), size = size * 0.6f, tint = Color.Unspecified)
        }
    }
}
