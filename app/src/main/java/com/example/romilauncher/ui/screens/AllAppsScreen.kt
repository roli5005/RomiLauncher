package com.example.romilauncher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.example.romilauncher.data.AppSort
import com.example.romilauncher.data.LongPressAction
import com.example.romilauncher.ui.DialogState
import com.example.romilauncher.ui.RomiViewModel
import com.example.romilauncher.ui.components.AppTile
import com.example.romilauncher.ui.components.EmptyState
import com.example.romilauncher.ui.components.FocusSurface
import com.example.romilauncher.ui.components.RomiIcons
import com.example.romilauncher.ui.components.RomiTextField
import com.example.romilauncher.ui.components.autoFocus
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing
import com.example.romilauncher.ui.theme.RomiType

@Composable
fun AllAppsScreen(vm: RomiViewModel, modifier: Modifier = Modifier) {
    val settings = Romi.settings
    val apps by vm.allAppsSorted.collectAsStateWithLifecycle()
    val pinned by vm.pinnedApps.collectAsStateWithLifecycle()
    val loaded by vm.appsLoaded.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val pinnedKeys = remember(pinned) { pinned.map { it.packageName }.toSet() }
    val shown = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    Column(modifier.fillMaxSize().padding(end = RomiSpacing.gutter)) {
        Row(
            Modifier.padding(bottom = RomiSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RomiSpacing.m),
        ) {
            Text("All Apps", style = RomiType.section, color = Romi.colors.textPrimary)
            RomiTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Filter apps",
                icon = RomiIcons.search,
                modifier = Modifier.weight(1f),
            )
            FocusSurface(
                onClick = {
                    vm.update {
                        it.copy(appSort = if (it.appSort == AppSort.ALPHABETICAL) AppSort.FAVORITES_FIRST else AppSort.ALPHABETICAL)
                    }
                },
                shape = CircleShape,
                description = "Sort order: ${settings.appSort.label}. Select to change.",
            ) {
                Text(
                    "Sort: ${settings.appSort.label}",
                    style = RomiType.label,
                    modifier = Modifier.padding(horizontal = RomiSpacing.m, vertical = 10.dp),
                )
            }
        }
        when {
            !loaded -> Unit
            shown.isEmpty() -> EmptyState(
                if (query.isBlank()) "No apps found" else "No matches",
                if (query.isBlank()) "Romi didn't find any launchable apps on this TV." else "No app name contains \"${query.trim()}\".",
                icon = RomiIcons.search,
            )
            else -> LazyVerticalGrid(
                columns = GridCells.FixedSize(136.dp * settings.density.scale),
                contentPadding = PaddingValues(6.dp),
                horizontalArrangement = Arrangement.spacedBy(RomiSpacing.m - 4.dp),
                verticalArrangement = Arrangement.spacedBy(RomiSpacing.m - 4.dp),
            ) {
                itemsIndexed(shown, key = { _, app -> app.packageName }) { index, app ->
                    AppTile(
                        app = app,
                        width = 136.dp * settings.density.scale,
                        onClick = { vm.launch(app) },
                        onLongClick = when (settings.longPress) {
                            LongPressAction.MENU -> ({ vm.dialog = DialogState.AppMenu(app.packageName) })
                            LongPressAction.TOGGLE_PIN -> ({ vm.togglePin(app) })
                            LongPressAction.NONE -> null
                        },
                        pinned = app.packageName in pinnedKeys,
                        showPinBadge = true,
                        modifier = if (index == 0 && query.isBlank()) Modifier.autoFocus() else Modifier,
                    )
                }
            }
        }
    }
}
