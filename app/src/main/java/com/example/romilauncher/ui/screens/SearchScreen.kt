package com.example.romilauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.example.romilauncher.data.AppEntry
import com.example.romilauncher.data.MediaItem
import com.example.romilauncher.data.ShortcutAction
import com.example.romilauncher.data.SystemDestination
import com.example.romilauncher.ui.RomiViewModel
import com.example.romilauncher.ui.Route
import com.example.romilauncher.ui.SettingsPage
import com.example.romilauncher.ui.components.AppIconImage
import com.example.romilauncher.ui.components.EmptyState
import com.example.romilauncher.ui.components.FocusSurface
import com.example.romilauncher.ui.components.RomiIcon
import com.example.romilauncher.ui.components.RomiIcons
import com.example.romilauncher.ui.components.RomiTextField
import com.example.romilauncher.ui.components.autoFocus
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing
import com.example.romilauncher.ui.theme.RomiType
import kotlinx.coroutines.delay

private enum class ResultKind(val label: String, val heading: String) {
    APP("App", "Apps"),
    SHORTCUT("Shortcut", "Shortcuts"),
    SETTING("Setting", "Settings"),
    MEDIA("Media", "Media"),
}

private data class SearchResult(
    val key: String,
    val kind: ResultKind,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector? = null,
    val app: AppEntry? = null,
    val open: () -> Unit,
)

private class RomiSettingEntry(val title: String, val keywords: String, val page: SettingsPage)

private val ROMI_SETTINGS = listOf(
    RomiSettingEntry("Theme", "dark graphite black appearance", SettingsPage.APPEARANCE),
    RomiSettingEntry("Accent color", "colour blue teal amber", SettingsPage.APPEARANCE),
    RomiSettingEntry("Background and wallpaper", "image glow", SettingsPage.APPEARANCE),
    RomiSettingEntry("Background dimming", "dark brightness", SettingsPage.APPEARANCE),
    RomiSettingEntry("High contrast", "accessibility readable", SettingsPage.APPEARANCE),
    RomiSettingEntry("Card size", "density layout compact large", SettingsPage.APPEARANCE),
    RomiSettingEntry("Animations", "motion reduce", SettingsPage.APPEARANCE),
    RomiSettingEntry("Home sections", "continue watching trending apps show hide", SettingsPage.HOME_SCREEN),
    RomiSettingEntry("Restore default layout", "reset home", SettingsPage.HOME_SCREEN),
    RomiSettingEntry("Sidebar shortcuts", "add rename reorder remove", SettingsPage.SHORTCUTS),
    RomiSettingEntry("Default focus", "start navigation", SettingsPage.NAVIGATION),
    RomiSettingEntry("Sidebar behavior", "collapse expand", SettingsPage.NAVIGATION),
    RomiSettingEntry("Focus appearance", "outline glow", SettingsPage.NAVIGATION),
    RomiSettingEntry("Long-press behavior", "hold menu pin", SettingsPage.NAVIGATION),
    RomiSettingEntry("Clock format", "12-hour 24-hour time", SettingsPage.CLOCK),
)

/** Whole-word prefix match, so "you" finds "YouTube" but not "layout". */
private fun String.hasWordStartingWith(prefix: String): Boolean =
    split(' ', '-', '/').any { it.startsWith(prefix, ignoreCase = true) } || startsWith(prefix, ignoreCase = true)

@Composable
fun SearchScreen(vm: RomiViewModel, modifier: Modifier = Modifier) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()

    // Media search is the only async source, and it can be empty or slow, so it is merged in late.
    val media by produceState(emptyList<MediaItem>(), q) {
        value = emptyList()
        if (q.isNotEmpty()) {
            delay(250)
            value = vm.searchMedia(q)
        }
    }

    val results = remember(q, apps, shortcuts, media) {
        if (q.isEmpty()) return@remember emptyList()
        fun rank(text: String) = if (text.startsWith(q, ignoreCase = true)) 0 else 1
        buildList {
            addAll(apps.filter { it.label.contains(q, true) }.sortedBy { rank(it.label) }.map { app ->
                SearchResult("app:${app.packageName}", ResultKind.APP, app.label, null, app = app, open = { vm.launch(app) })
            })
            addAll(shortcuts.filter { it.shortcut.label.contains(q, true) }.map { item ->
                SearchResult(
                    "shortcut:${item.shortcut.id}", ResultKind.SHORTCUT, item.shortcut.label,
                    if (item.available) null else "Unavailable on this TV",
                    icon = RomiIcons.get(item.shortcut.iconKey.takeUnless { it == "app" } ?: "star"),
                    open = { vm.run(item) },
                )
            })
            addAll(ROMI_SETTINGS.filter { it.title.hasWordStartingWith(q) || it.keywords.hasWordStartingWith(q) }.map { e ->
                SearchResult(
                    "romi:${e.title}", ResultKind.SETTING, e.title, "Romi settings, ${e.page.label}",
                    icon = RomiIcons.settings, open = { vm.go(Route.Settings(e.page)) },
                )
            })
            addAll(SystemDestination.entries.filter { it.title.hasWordStartingWith(q) && vm.systemAvailable(it) }.map { d ->
                SearchResult(
                    "system:${d.name}", ResultKind.SETTING, d.title, "Android settings",
                    icon = RomiIcons.get(d.icon), open = { vm.openSystem(d) },
                )
            })
            addAll(media.map { item ->
                SearchResult(
                    "media:${item.sourceId}:${item.id}", ResultKind.MEDIA, item.title, item.subtitle,
                    icon = RomiIcons.play, open = { vm.openMedia(item) },
                )
            })
        }
    }

    Column(modifier.fillMaxSize().padding(end = RomiSpacing.gutter)) {
        RomiTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search apps, shortcuts, settings and media",
            icon = RomiIcons.search,
            imeAction = ImeAction.Search,
            modifier = Modifier.autoFocus(),
        )
        when {
            q.isEmpty() -> EmptyState(
                "Search Romi",
                "Type to search. Press Back to close the keyboard and reach the results.",
                icon = RomiIcons.search,
            )
            results.isEmpty() -> EmptyState(
                "No results",
                "Nothing matches \"$q\". Media results need a connected content source, and none is set up.",
                icon = RomiIcons.search,
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(top = RomiSpacing.m),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = RomiSpacing.l),
            ) {
                ResultKind.entries.forEach { kind ->
                    val group = results.filter { it.kind == kind }
                    if (group.isNotEmpty()) {
                        item(key = "h-${kind.name}") {
                            Text(
                                kind.heading, style = RomiType.label, color = Romi.colors.textSecondary,
                                modifier = Modifier.padding(top = RomiSpacing.s, start = 4.dp),
                            )
                        }
                        items(group, key = { it.key }) { result -> ResultRow(result) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult) {
    val colors = Romi.colors
    FocusSurface(
        onClick = result.open,
        shape = RoundedCornerShape(12.dp),
        focusedScale = 1.01f,
        description = "${result.kind.label}: ${result.title}",
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val app = result.app
            if (app != null) AppIconImage(app, 32.dp)
            else RomiIcon(result.icon ?: RomiIcons.info, size = 24.dp)
            Column(Modifier.weight(1f)) {
                Text(result.title, style = RomiType.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                result.subtitle?.let {
                    Text(it, style = RomiType.label, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.background(colors.surface, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(result.kind.label, style = RomiType.label, color = colors.textSecondary)
            }
        }
    }
}
