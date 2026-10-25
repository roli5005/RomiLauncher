package com.example.romilauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.example.romilauncher.data.DefaultFocus
import com.example.romilauncher.data.ShortcutAction
import com.example.romilauncher.data.SidebarMode
import com.example.romilauncher.ui.components.AddShortcutDialog
import com.example.romilauncher.ui.components.AppMenuDialog
import com.example.romilauncher.ui.components.ConfirmDialog
import com.example.romilauncher.ui.components.IconPickerDialog
import com.example.romilauncher.ui.components.LocalAppImageLoader
import com.example.romilauncher.ui.components.LocalFocusMemory
import com.example.romilauncher.ui.components.RomiBackground
import com.example.romilauncher.ui.components.ShortcutMenuDialog
import com.example.romilauncher.ui.components.Sidebar
import com.example.romilauncher.ui.components.TextInputDialog
import com.example.romilauncher.ui.components.TopBar
import com.example.romilauncher.ui.screens.AllAppsScreen
import com.example.romilauncher.ui.screens.ContentTabScreen
import com.example.romilauncher.ui.screens.HomeScreen
import com.example.romilauncher.ui.screens.SearchScreen
import com.example.romilauncher.ui.screens.SettingsScreen
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing
import com.example.romilauncher.ui.theme.RomiTheme
import com.example.romilauncher.ui.theme.RomiType
import kotlinx.coroutines.delay

@Composable
fun RomiRoot(vm: RomiViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    RomiTheme(settings) {
        CompositionLocalProvider(
            LocalAppImageLoader provides vm.images,
            LocalFocusMemory provides vm.focusMemory,
        ) {
            RomiScaffold(vm)
        }
    }
}

@Composable
private fun RomiScaffold(vm: RomiViewModel) {
    val settings = Romi.settings
    val route = vm.route
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()
    val appsLoaded by vm.appsLoaded.collectAsStateWithLifecycle()

    val compact = settings.sidebarMode == SidebarMode.COMPACT
    val sidebarWidth = if (compact) RomiSpacing.sidebarCompact else RomiSpacing.sidebarExpanded

    val sidebarHome = remember { FocusRequester() }
    val topHome = remember { FocusRequester() }
    val appsFirst = remember { FocusRequester() }
    val memory = vm.focusMemory

    val focusDefault: () -> Unit = {
        val tab = (route as? Route.Home)?.tab ?: HomeTab.HOME
        val order = when {
            tab != HomeTab.HOME -> listOf(topHome)
            settings.defaultFocus == DefaultFocus.APPS -> listOf(appsFirst, sidebarHome)
            settings.defaultFocus == DefaultFocus.SIDEBAR -> listOf(sidebarHome)
            else -> listOf(topHome, sidebarHome)
        }
        order.firstOrNull { runCatching { it.requestFocus() }.getOrDefault(false) }
    }

    // First launch: wait until apps exist, then land focus on the configured target.
    var initialFocusDone by remember { mutableStateOf(false) }
    LaunchedEffect(appsLoaded) {
        if (!initialFocusDone && appsLoaded && route is Route.Home && memory.pending == null) {
            delay(150)
            focusDefault()
            initialFocusDone = true
        }
    }
    // Returning to Home: if the remembered element no longer exists, fall back to the default target.
    LaunchedEffect(route) {
        if (route is Route.Home && memory.pending != null) {
            delay(600)
            if (memory.pending != null) {
                memory.pending = null
                focusDefault()
            }
        }
    }
    // Pressing the Home button while Romi is open resets to the home screen.
    LaunchedEffect(vm.homeSignal) {
        if (vm.homeSignal > 0) {
            delay(200)
            focusDefault()
        }
    }

    BackHandler {
        if (!vm.back()) focusDefault()
    }

    Box(Modifier.fillMaxSize()) {
        RomiBackground(vm::loadWallpaper)
        Column(Modifier.fillMaxSize()) {
            TopBar(
                logoWidth = sidebarWidth,
                activeTab = (route as? Route.Home)?.tab,
                onTab = { vm.go(Route.Home(it)) },
                onSearch = { vm.go(Route.Search) },
                onSettings = { vm.go(Route.Settings()) },
                tabRequester = topHome,
            )
            Box(Modifier.weight(1f)) {
                Row(Modifier.fillMaxSize()) {
                    Spacer(Modifier.width(sidebarWidth))
                    Crossfade(
                        targetState = route,
                        animationSpec = tween(if (Romi.motion.animated) Romi.motion.focusMs else 0),
                        modifier = Modifier.weight(1f),
                        label = "route",
                    ) { target ->
                        when (target) {
                            is Route.Home -> if (target.tab == HomeTab.HOME) {
                                HomeScreen(vm, appsFirst)
                            } else {
                                ContentTabScreen(target.tab, vm)
                            }
                            Route.AllApps -> AllAppsScreen(vm)
                            Route.Search -> SearchScreen(vm)
                            is Route.Settings -> SettingsScreen(vm, target.page)
                        }
                    }
                }
                Sidebar(
                    route = route,
                    mode = settings.sidebarMode,
                    shortcuts = shortcuts,
                        onHome = {
                        if (route is Route.Home && (route as Route.Home).tab == HomeTab.HOME) focusDefault() else vm.goHome()
                    },
                    onSearch = { vm.go(Route.Search) },
                    onDisplayMode = vm::openDisplayMode,
                    onTimer = vm::openOffTimer,
                        onShortcut = vm::run,
                    onShortcutMenu = { vm.dialog = DialogState.ShortcutMenu(it.shortcut.id) },
                    onAddShortcut = { vm.dialog = DialogState.AddShortcut },
                    homeRequester = sidebarHome,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }
        vm.notice?.let { NoticeBar(it, Modifier.align(Alignment.BottomCenter)) }
        DialogHost(vm)
    }
}

@Composable
private fun NoticeBar(text: String, modifier: Modifier = Modifier) {
    val colors = Romi.colors
    Box(
        modifier
            .padding(bottom = RomiSpacing.xl)
            .background(colors.cardFocused, CircleShape)
            .padding(horizontal = RomiSpacing.l, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(text, style = RomiType.body, color = colors.textPrimary)
    }
}

@Composable
private fun DialogHost(vm: RomiViewModel) {
    val state = vm.dialog ?: return
    val dismiss = { vm.dialog = null }
    val apps by vm.apps.collectAsStateWithLifecycle()
    val pinned by vm.pinnedApps.collectAsStateWithLifecycle()
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()

    when (state) {
        is DialogState.AppMenu -> {
            val app = apps.firstOrNull { it.packageName == state.packageName }
            if (app == null) {
                dismiss()
            } else {
                val index = pinned.indexOfFirst { it.packageName == app.packageName }
                AppMenuDialog(
                    app = app,
                    pinned = index >= 0,
                    canMoveLeft = index > 0,
                    canMoveRight = index in 0 until pinned.lastIndex,
                    onOpen = { vm.launch(app) },
                    onTogglePin = { vm.togglePin(app) },
                    onMove = { vm.movePinned(app, it) },
                    onInfo = { vm.appInfo(app) },
                    onUninstall = { vm.uninstall(app) },
                    onDismiss = dismiss,
                )
            }
        }
        is DialogState.ShortcutMenu -> {
            val index = shortcuts.indexOfFirst { it.shortcut.id == state.id }
            if (index < 0) {
                dismiss()
            } else {
                val item = shortcuts[index]
                ShortcutMenuDialog(
                    item = item,
                    canMoveUp = index > 0,
                    canMoveDown = index < shortcuts.lastIndex,
                    onRename = { vm.dialog = DialogState.RenameShortcut(state.id) },
                    onIcon = { vm.dialog = DialogState.IconPicker(state.id) },
                    onMove = { vm.moveShortcut(state.id, it) },
                    onRemove = {
                        vm.confirm("Remove ${item.shortcut.label}?", "It will disappear from the sidebar.", "Remove") {
                            vm.removeShortcut(state.id)
                        }
                    },
                    onDismiss = dismiss,
                )
            }
        }
        is DialogState.RenameShortcut -> {
            val item = shortcuts.firstOrNull { it.shortcut.id == state.id }
            if (item == null) dismiss() else TextInputDialog(
                title = "Rename shortcut",
                initial = item.shortcut.label,
                onSave = { vm.renameShortcut(state.id, it) },
                onDismiss = dismiss,
            )
        }
        is DialogState.IconPicker -> {
            val item = shortcuts.firstOrNull { it.shortcut.id == state.id }
            if (item == null) dismiss() else IconPickerDialog(
                current = item.shortcut.iconKey,
                appIconAvailable = item.shortcut.action is ShortcutAction.OpenApp,
                onPick = { vm.setShortcutIcon(state.id, it); dismiss() },
                onDismiss = dismiss,
            )
        }
        DialogState.AddShortcut -> AddShortcutDialog(
            apps = apps,
            isAvailable = vm::systemAvailable,
            onAdd = vm::addShortcut,
            onDismiss = dismiss,
        )
        is DialogState.Confirm -> ConfirmDialog(
            title = state.title,
            message = state.message,
            confirmLabel = state.confirmLabel,
            onConfirm = state.onConfirm,
            onDismiss = dismiss,
        )
    }
}
