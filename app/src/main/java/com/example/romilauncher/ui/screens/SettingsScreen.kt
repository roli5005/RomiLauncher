package com.example.romilauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.example.romilauncher.data.AccentId
import com.example.romilauncher.data.BackgroundId
import com.example.romilauncher.data.Labeled
import com.example.romilauncher.data.RomiSettings
import com.example.romilauncher.ui.ActivityRequest
import com.example.romilauncher.ui.DialogState
import com.example.romilauncher.ui.RomiViewModel
import com.example.romilauncher.ui.Route
import com.example.romilauncher.ui.SettingsPage
import com.example.romilauncher.ui.components.Clock
import com.example.romilauncher.ui.components.DialogButton
import com.example.romilauncher.ui.components.FocusSurface
import com.example.romilauncher.ui.components.RomiIcon
import com.example.romilauncher.ui.components.RomiIcons
import com.example.romilauncher.ui.components.ShortcutGlyph
import com.example.romilauncher.ui.components.autoFocus
import com.example.romilauncher.ui.components.formatClock
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing
import com.example.romilauncher.ui.theme.RomiType
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun SettingsScreen(vm: RomiViewModel, initialPage: SettingsPage, modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(initialPage) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    Row(modifier.fillMaxSize().padding(end = RomiSpacing.gutter), horizontalArrangement = Arrangement.spacedBy(RomiSpacing.l)) {
        Column(Modifier.width(220.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Settings", style = RomiType.section, color = Romi.colors.textPrimary, modifier = Modifier.padding(bottom = RomiSpacing.s))
            SettingsPage.entries.forEach { entry ->
                val selected = entry == page
                FocusSurface(
                    onClick = { page = entry },
                    shape = RoundedCornerShape(12.dp),
                    container = if (selected) Romi.colors.card else Color.Transparent,
                    contentColor = if (selected) Romi.colors.textPrimary else Romi.colors.textSecondary,
                    focusedScale = 1.02f,
                    description = if (selected) "${entry.label}, selected" else entry.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (entry == initialPage) it.autoFocus() else it }
                        // Focusing a category shows its page, as on the Android TV settings app.
                        .onFocusChanged { if (it.hasFocus) page = entry },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RomiIcon(RomiIcons.get(entry.icon), size = 22.dp, tint = Color.Unspecified)
                        Text(entry.label, style = RomiType.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = RomiSpacing.l),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (page) {
                SettingsPage.APPEARANCE -> AppearancePage(vm, settings)
                SettingsPage.HOME_SCREEN -> HomeScreenPage(vm, settings) { page = it }
                SettingsPage.SHORTCUTS -> ShortcutsPage(vm)
                SettingsPage.NAVIGATION -> NavigationPage(vm, settings)
                SettingsPage.CLOCK -> ClockPage(vm, settings)
                SettingsPage.ABOUT -> AboutPage(vm)
            }
        }
    }
}

@Composable
private fun AppearancePage(vm: RomiViewModel, s: RomiSettings) {
    CycleRow("Theme", s.theme, "Dark is the default.") { next -> vm.update { it.copy(theme = next) } }
    AccentRow(s.accent) { next -> vm.update { it.copy(accent = next) } }
    CycleRow("Background", s.background) { next -> vm.update { it.copy(background = next) } }
    if (s.background == BackgroundId.CUSTOM) {
        ActionRow(
            "Choose background image", "The default glow shows until you pick one. Needs a file picker, which some TVs lack.",
            icon = RomiIcons.palette,
        ) { vm.request(ActivityRequest.PickWallpaper) }
    }
    val steps = listOf(0, 10, 20, 30, 40, 50, 60)
    SettingRow("Background dimming", "${s.dimming}%", "Darkens the background behind content. Select to change.") {
        val next = steps.firstOrNull { it > s.dimming } ?: steps.first()
        vm.update { it.copy(dimming = next) }
    }
    ToggleRow("High contrast", s.highContrast, "Brighter secondary text, stronger outlines.") { on ->
        vm.update { it.copy(highContrast = on) }
    }
    CycleRow("Card size", s.density) { next -> vm.update { it.copy(density = next) } }
    CycleRow(
        "Animations", s.animations,
        "Reduced and Off remove the focus zoom. Android's own animation setting always applies.",
    ) { next -> vm.update { it.copy(animations = next) } }
}

@Composable
private fun HomeScreenPage(vm: RomiViewModel, s: RomiSettings, openPage: (SettingsPage) -> Unit) {
    val content by vm.content.collectAsStateWithLifecycle()
    val pinned by vm.pinnedApps.collectAsStateWithLifecycle()
    val noSource = if (content.configured) null else "No content source is connected, so this section stays hidden."
    ToggleRow("Your Apps", s.showApps) { on -> vm.update { it.copy(showApps = on) } }
    ToggleRow("Continue Watching", s.showContinueWatching, noSource) { on -> vm.update { it.copy(showContinueWatching = on) } }
    ToggleRow("Trending Now", s.showTrending, noSource) { on -> vm.update { it.copy(showTrending = on) } }
    CycleRow("All Apps order", s.appSort) { next -> vm.update { it.copy(appSort = next) } }
    SettingRow("Pinned apps", "${pinned.size}", "Open All Apps. Hold Select on an app to pin, unpin or reorder.") {
        vm.go(Route.AllApps)
    }
    SettingRow("Sidebar shortcuts", null, "Add, rename, reorder and remove.") { openPage(SettingsPage.SHORTCUTS) }
    ActionRow("Restore default layout", "Resets pinned apps, sections, card size and sort order.", danger = true) {
        vm.confirm(
            "Restore default layout?",
            "Pinned apps, section visibility, card size and sort order go back to their defaults. Shortcuts are kept.",
            "Restore",
        ) { vm.restoreDefaultLayout() }
    }
}

@Composable
private fun ShortcutsPage(vm: RomiViewModel) {
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()
    if (shortcuts.isEmpty()) {
        Text("No shortcuts yet.", style = RomiType.body, color = Romi.colors.textSecondary)
    }
    shortcuts.forEach { item ->
        SettingRow(
            title = item.shortcut.label,
            value = null,
            supporting = if (item.available) "Select to rename, change icon, reorder or remove." else "Unavailable on this TV",
            leading = { ShortcutGlyph(item, 32.dp) },
        ) { vm.dialog = DialogState.ShortcutMenu(item.shortcut.id) }
    }
    ActionRow("Add shortcut", null, icon = RomiIcons.add) { vm.dialog = DialogState.AddShortcut }
    ActionRow("Restore default shortcuts", "Replaces your shortcuts with the defaults.", danger = true) {
        vm.confirm(
            "Restore default shortcuts?",
            "Your custom shortcuts will be removed and replaced with the defaults.",
            "Restore",
        ) { vm.restoreDefaultShortcuts() }
    }
}

@Composable
private fun NavigationPage(vm: RomiViewModel, s: RomiSettings) {
    CycleRow("Default focus", s.defaultFocus, "Where focus lands when Romi opens or Home is pressed.") { next ->
        vm.update { it.copy(defaultFocus = next) }
    }
    CycleRow("Sidebar behavior", s.sidebarMode) { next -> vm.update { it.copy(sidebarMode = next) } }
    CycleRow("Focus appearance", s.focusStyle, "Focus always shows an outline and a lighter fill.") { next ->
        vm.update { it.copy(focusStyle = next) }
    }
    CycleRow("Long-press on apps", s.longPress, "Hold Select on an app tile.") { next ->
        vm.update { it.copy(longPress = next) }
    }
}

@Composable
private fun ClockPage(vm: RomiViewModel, s: RomiSettings) {
    val context = LocalContext.current
    CycleRow("Time format", s.clockFormat) { next -> vm.update { it.copy(clockFormat = next) } }
    val zone = ZoneId.systemDefault()
    val preview = formatClock(context, ZonedDateTime.now(zone), s.clockFormat)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(RomiSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Clock()
        Text(
            "Now $preview in ${zone.id}. The clock follows this TV's time zone and language.",
            style = RomiType.label, color = Romi.colors.textSecondary,
        )
    }
}

@Composable
private fun AboutPage(vm: RomiViewModel) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "unknown"
    }
    SettingRow("Romi", version, "Android TV launcher") { }
    ActionRow("Make Romi the home app", "Opens the system chooser for the Home button.", icon = RomiIcons.home) {
        vm.request(ActivityRequest.SetDefaultHome)
    }
    InfoBlock(
        "Permissions",
        "Romi reads the list of launchable apps and opens system settings screens, including the TV's own " +
            "off timer. It never uses root or hidden system interfaces.",
    )
}

// Row building blocks

@Composable
private fun remember(vararg keys: Any?, calc: () -> String): String =
    androidx.compose.runtime.remember(*keys) { calc() }

@Composable
private fun SettingRow(
    title: String,
    value: String?,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = Romi.colors
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        focusedScale = 1.01f,
        description = listOfNotNull(title, value, supporting).joinToString(". "),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f)) {
                Text(title, style = RomiType.body)
                if (supporting != null) {
                    Text(supporting, style = RomiType.label, color = colors.textSecondary)
                }
            }
            if (value != null) {
                Text(value, style = RomiType.bodyStrong, color = colors.accent, maxLines = 1)
            }
            RomiIcon(RomiIcons.chevron, size = 20.dp, tint = colors.textSecondary)
        }
    }
}

@Composable
private fun <E> CycleRow(title: String, current: E, supporting: String? = null, onChange: (E) -> Unit)
    where E : Enum<E>, E : Labeled {
    SettingRow(title, current.label, supporting ?: "Select to change.") {
        val all = current.declaringJavaClass.enumConstants
        onChange(all[(current.ordinal + 1) % all.size])
    }
}

@Composable
private fun ActionRow(
    title: String,
    supporting: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = Romi.colors
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentColor = if (danger) colors.danger else colors.textPrimary,
        focusedScale = 1.01f,
        description = listOfNotNull(title, supporting).joinToString(". "),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (icon != null) RomiIcon(icon, size = 22.dp, tint = Color.Unspecified)
            Column(Modifier.weight(1f)) {
                Text(title, style = RomiType.body)
                if (supporting != null) Text(supporting, style = RomiType.label, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, supporting: String? = null, onChange: (Boolean) -> Unit) {
    val colors = Romi.colors
    FocusSurface(
        onClick = { onChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        focusedScale = 1.01f,
        description = "$title, ${if (checked) "on" else "off"}",
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = RomiType.body)
                if (supporting != null) Text(supporting, style = RomiType.label, color = colors.textSecondary)
            }
            // The word, not just the colour, carries the state.
            Text(if (checked) "On" else "Off", style = RomiType.bodyStrong, color = if (checked) colors.accent else colors.textSecondary)
            Box(
                Modifier.size(width = 44.dp, height = 24.dp)
                    .background(if (checked) colors.accent else colors.cardFocused, CircleShape)
                    .border(1.dp, colors.divider, CircleShape)
                    .padding(3.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(Modifier.size(18.dp).background(if (checked) colors.onAccent else colors.textSecondary, CircleShape))
            }
        }
    }
}

@Composable
private fun AccentRow(current: AccentId, onChange: (AccentId) -> Unit) {
    val colors = Romi.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Accent color  ·  ${current.label}", style = RomiType.body, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentId.entries.forEach { accent ->
                val selected = accent == current
                FocusSurface(
                    onClick = { onChange(accent) },
                    shape = CircleShape,
                    container = Color(accent.argb),
                    focusedContainer = Color(accent.argb),
                    description = if (selected) "${accent.label}, selected" else accent.label,
                    modifier = Modifier.size(40.dp),
                ) {
                    if (selected) {
                        RomiIcon(RomiIcons.check, Modifier.align(Alignment.Center), size = 22.dp, tint = Color(0xFF0B0F14))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = RomiType.bodyStrong, color = Romi.colors.textPrimary)
        Text(text, style = RomiType.label, color = Romi.colors.textSecondary)
    }
}
