package com.example.romilauncher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.romilauncher.data.LongPressAction
import com.example.romilauncher.data.MediaSection
import com.example.romilauncher.ui.DialogState
import com.example.romilauncher.ui.HomeTab
import com.example.romilauncher.ui.RomiViewModel
import com.example.romilauncher.ui.Route
import com.example.romilauncher.ui.components.AllAppsTile
import com.example.romilauncher.ui.components.AppTile
import com.example.romilauncher.ui.components.EmptyState
import com.example.romilauncher.ui.components.LocalFocusMemory
import com.example.romilauncher.ui.components.MediaCard
import com.example.romilauncher.ui.components.RomiIcons
import com.example.romilauncher.ui.components.SectionHeader
import com.example.romilauncher.ui.components.homeFocus
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing

@Composable
fun HomeScreen(vm: RomiViewModel, appsFirstRequester: FocusRequester, modifier: Modifier = Modifier) {
    val settings = Romi.settings
    val pinned by vm.pinnedApps.collectAsStateWithLifecycle()
    val appsLoaded by vm.appsLoaded.collectAsStateWithLifecycle()
    val content by vm.content.collectAsStateWithLifecycle()
    val scale = settings.density.scale

    val continueItems = content.items(MediaSection.CONTINUE_WATCHING)
    val trendingItems = content.items(MediaSection.TRENDING)
    val showContinue = settings.showContinueWatching && continueItems.isNotEmpty()
    val showTrending = settings.showTrending && trendingItems.isNotEmpty()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = RomiSpacing.gutter, bottom = RomiSpacing.l),
        verticalArrangement = Arrangement.spacedBy(RomiSpacing.l),
    ) {
        if (settings.showApps) {
            Column {
                SectionHeader("Your Apps")
                // Not composed before the scan finishes. A lazy row anchors on its item keys, so a row that
                // starts with only "All Apps" and then gains apps in front of it would stay scrolled to the end.
                if (appsLoaded) AppsRow(vm, pinned, scale, appsFirstRequester)
            }
        }
        // Sections without a real data source stay hidden instead of showing invented content.
        if (showContinue) {
            Column {
                SectionHeader("Continue Watching")
                MediaRow(vm, continueItems, 208.dp * scale, "continue")
            }
        }
        if (showTrending) {
            Column {
                SectionHeader("Trending Now")
                MediaRow(vm, trendingItems, 144.dp * scale, "trending")
            }
        }
        if (!settings.showApps && !showContinue && !showTrending) {
            EmptyState(
                "Nothing to show",
                "Every section is hidden. Turn them back on in Settings, under Home screen.",
                icon = RomiIcons.settings,
            )
        }
    }
}

@Composable
private fun AppsRow(
    vm: RomiViewModel,
    pinned: List<com.example.romilauncher.data.AppEntry>,
    scale: Float,
    firstRequester: FocusRequester,
) {
    val settings = Romi.settings
    val memory = LocalFocusMemory.current
    val state = rememberLazyListState()
    val tileWidth = 136.dp * scale

    // The row is lazy, so a restore target further along must be scrolled into composition first.
    LaunchedEffect(pinned.size) {
        val target = memory.pending ?: return@LaunchedEffect
        val index = when {
            target == "app:all" -> pinned.size
            target.startsWith("app:") -> pinned.indexOfFirst { "app:${it.packageName}" == target }
            else -> -1
        }
        if (index >= 0) state.scrollToItem(index)
    }

    LazyRow(
        state = state,
        modifier = Modifier.focusRestorer(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(RomiSpacing.m - 4.dp),
    ) {
        items(pinned, key = { it.packageName }) { app ->
            val first = app.packageName == pinned.first().packageName
            AppTile(
                app = app,
                width = tileWidth,
                onClick = { vm.launch(app) },
                onLongClick = when (settings.longPress) {
                    LongPressAction.MENU -> ({ vm.dialog = DialogState.AppMenu(app.packageName) })
                    LongPressAction.TOGGLE_PIN -> ({ vm.togglePin(app) })
                    LongPressAction.NONE -> null
                },
                pinned = true,
                modifier = Modifier.homeFocus("app:${app.packageName}", if (first) firstRequester else null),
            )
        }
        item(key = "all-apps") {
            AllAppsTile(
                width = tileWidth,
                onClick = { vm.go(Route.AllApps) },
                modifier = Modifier.homeFocus("app:all", if (pinned.isEmpty()) firstRequester else null),
            )
        }
    }
}

@Composable
private fun MediaRow(
    vm: RomiViewModel,
    items: List<com.example.romilauncher.data.MediaItem>,
    cardWidth: androidx.compose.ui.unit.Dp,
    prefix: String,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(RomiSpacing.m - 4.dp),
    ) {
        items(items, key = { it.sourceId + it.id }) { item ->
            MediaCard(
                item = item,
                width = cardWidth,
                onClick = { vm.openMedia(item) },
                modifier = Modifier.homeFocus("$prefix:${item.sourceId}:${item.id}"),
            )
        }
    }
}

/** Movies, TV Shows and Library need a content provider. Without one they say so plainly. */
@Composable
fun ContentTabScreen(tab: HomeTab, vm: RomiViewModel, modifier: Modifier = Modifier) {
    val content by vm.content.collectAsStateWithLifecycle()
    val (section, name) = when (tab) {
        HomeTab.MOVIES -> MediaSection.MOVIES to "movies"
        HomeTab.TV_SHOWS -> MediaSection.TV_SHOWS to "TV shows"
        else -> MediaSection.LIBRARY to "your library"
    }
    val items = content.items(section)
    Column(modifier.fillMaxSize().padding(end = RomiSpacing.gutter)) {
        if (items.isEmpty()) {
            EmptyState(
                if (content.configured) "Nothing here yet" else "No content source connected",
                if (content.configured) {
                    "Your connected sources have no $name to show."
                } else {
                    "Romi lists $name from supported providers. None is set up on this TV, so there is nothing to show."
                },
            )
        } else {
            SectionHeader(tab.label)
            MediaRow(vm, items, 168.dp * Romi.settings.density.scale, tab.name.lowercase())
        }
    }
}
