package com.example.romilauncher.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.romilauncher.RomiApp
import com.example.romilauncher.data.AppEntry
import com.example.romilauncher.data.AppSort
import com.example.romilauncher.data.ContentUi
import com.example.romilauncher.data.MediaItem
import com.example.romilauncher.data.OpenResult
import com.example.romilauncher.data.RomiSettings
import com.example.romilauncher.data.RomiTarget
import com.example.romilauncher.data.Shortcut
import com.example.romilauncher.data.ShortcutAction
import com.example.romilauncher.data.SystemDestination
import com.example.romilauncher.data.SystemLauncher
import com.example.romilauncher.ui.components.FocusMemory
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class HomeTab(val label: String) {
    HOME("Home"), MOVIES("Movies"), TV_SHOWS("TV Shows"), LIBRARY("Library")
}

enum class SettingsPage(val label: String, val icon: String) {
    APPEARANCE("Appearance", "tune"),
    HOME_SCREEN("Home screen", "home"),
    SHORTCUTS("Sidebar shortcuts", "star"),
    NAVIGATION("Navigation", "display"),
    CLOCK("Clock", "clock"),
    ABOUT("About", "info"),
}

sealed interface Route {
    data class Home(val tab: HomeTab = HomeTab.HOME) : Route
    data object AllApps : Route
    data object Search : Route
    data class Settings(val page: SettingsPage = SettingsPage.APPEARANCE) : Route
}

sealed interface DialogState {
    data class AppMenu(val packageName: String) : DialogState
    data class ShortcutMenu(val id: String) : DialogState
    data object AddShortcut : DialogState
    data class RenameShortcut(val id: String) : DialogState
    data class IconPicker(val id: String) : DialogState
    data class Confirm(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val onConfirm: () -> Unit,
    ) : DialogState
}

/** One-shot requests the activity has to carry out, because they need an Activity. */
sealed interface ActivityRequest {
    data object PickWallpaper : ActivityRequest
    data object SetDefaultHome : ActivityRequest
}

data class ShortcutUi(val shortcut: Shortcut, val available: Boolean, val app: AppEntry?)

class RomiViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as RomiApp).container
    private val settingsStore = container.settings

    val focusMemory = FocusMemory()
    val images get() = container.images

    var route by mutableStateOf<Route>(Route.Home())
        private set
    var dialog by mutableStateOf<DialogState?>(null)
    var notice by mutableStateOf<String?>(null)
        private set

    private val _requests = Channel<ActivityRequest>(Channel.BUFFERED)
    val requests = _requests.receiveAsFlow()

    val settings: StateFlow<RomiSettings> = settingsStore.settings
    val apps: StateFlow<List<AppEntry>> = container.apps.apps
    val appsLoaded: StateFlow<Boolean> = container.apps.loaded
    val rawPinned: StateFlow<List<String>?> = settingsStore.pinned

    /** Pinned apps that are still installed, in the user's order. */
    val pinnedApps: StateFlow<List<AppEntry>> =
        combine(apps, settingsStore.pinned) { all, pinned ->
            val byPackage = all.associateBy { it.packageName }
            (pinned ?: defaultPinned(all)).mapNotNull { byPackage[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAppsSorted: StateFlow<List<AppEntry>> =
        combine(apps, pinnedApps, settings) { all, pinned, s ->
            if (s.appSort == AppSort.FAVORITES_FIRST) {
                val rank = pinned.withIndex().associate { it.value.packageName to it.index }
                all.sortedWith(compareBy { rank[it.packageName] ?: Int.MAX_VALUE })
            } else {
                all
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val shortcuts: StateFlow<List<ShortcutUi>> =
        combine(settingsStore.shortcuts, apps) { shortcuts, all ->
            val byPackage = all.associateBy { it.packageName }
            shortcuts.map { s ->
                when (val a = s.action) {
                    is ShortcutAction.OpenApp -> byPackage[a.packageName].let { ShortcutUi(s, it != null, it) }
                    is ShortcutAction.OpenSystem -> ShortcutUi(s, systemAvailable(a.destination), null)
                    is ShortcutAction.OpenRomi -> ShortcutUi(s, true, null)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val systemAvailability = HashMap<SystemDestination, Boolean>()

    fun systemAvailable(destination: SystemDestination): Boolean =
        systemAvailability.getOrPut(destination) { SystemLauncher.isAvailable(getApplication(), destination) }

    private val _content = MutableStateFlow(ContentUi())
    val content: StateFlow<ContentUi> = _content

    init {
        refreshContent()
    }

    fun refreshContent() {
        viewModelScope.launch { _content.value = container.content.snapshot() }
    }

    suspend fun searchMedia(query: String): List<MediaItem> = container.content.search(query)

    // Navigation

    fun go(target: Route) {
        val from = route
        if (from is Route.Home && target !is Route.Home) {
            // focusMemory.last already points at the element that opened this screen.
        } else if (from !is Route.Home && target is Route.Home) {
            focusMemory.pending = focusMemory.last
        }
        route = target
    }

    fun goHome() = go(Route.Home())

    /** Bumped when the Home button is pressed while Romi is already running, so the UI can reset focus. */
    var homeSignal by mutableStateOf(0)
        private set

    fun onHomePressed() {
        dialog = null
        route = Route.Home()
        focusMemory.pending = null
        homeSignal++
    }

    /** Back button. Returns false when there is nothing left to step back to. */
    fun back(): Boolean = when (val r = route) {
        is Route.Home -> if (r.tab != HomeTab.HOME) { route = Route.Home(); true } else false
        else -> { goHome(); true }
    }

    fun openRomiTarget(target: RomiTarget) = when (target) {
        RomiTarget.SETTINGS -> go(Route.Settings())
        RomiTarget.SLEEP_TIMER -> openOffTimer()
        RomiTarget.ALL_APPS -> go(Route.AllApps)
        RomiTarget.SEARCH -> go(Route.Search)
    }

    // Notices

    private var noticeJob: Job? = null

    fun showNotice(message: String) {
        notice = message
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(3500)
            notice = null
        }
    }

    // Apps

    fun launch(app: AppEntry) {
        if (!container.apps.launch(app.packageName)) {
            showNotice("${app.label} couldn't be opened. It may have been removed or disabled.")
            container.apps.refresh()
        }
    }

    fun togglePin(app: AppEntry) {
        val current = currentPinned()
        val pinned = app.packageName in current
        settingsStore.setPinned(if (pinned) current - app.packageName else current + app.packageName)
        showNotice(if (pinned) "${app.label} removed from Home" else "${app.label} pinned to Home")
    }

    /** Moves a pinned app one place left (-1) or right (+1) among the apps that are actually shown. */
    fun movePinned(app: AppEntry, delta: Int) {
        val visible = pinnedApps.value.map { it.packageName }
        val from = visible.indexOf(app.packageName)
        val to = from + delta
        if (from < 0 || to !in visible.indices) return
        val reordered = visible.toMutableList().apply { add(to, removeAt(from)) }
        // Pins of apps that are currently uninstalled keep their place at the end.
        val hidden = currentPinned().filter { it !in visible }
        settingsStore.setPinned(reordered + hidden)
    }

    private fun currentPinned(): List<String> =
        settingsStore.pinned.value ?: pinnedApps.value.map { it.packageName }

    fun appInfo(app: AppEntry) = openIntent(
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")),
        "App info isn't available on this TV.",
    )

    fun uninstall(app: AppEntry) = openIntent(
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")),
        "${app.label} can't be uninstalled from here.",
    )

    private fun openIntent(intent: Intent, failure: String) {
        runCatching { getApplication<Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { showNotice(failure) }
    }

    fun openMedia(item: MediaItem) {
        val uri = item.launchUri ?: return showNotice("${item.title} has no way to open from here.")
        runCatching {
            getApplication<Application>().startActivity(
                Intent.parseUri(uri, Intent.URI_INTENT_SCHEME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { showNotice("${item.title} couldn't be opened.") }
    }

    // System settings

    fun openSystem(destination: SystemDestination) {
        when (val result = SystemLauncher.open(getApplication(), destination)) {
            OpenResult.Opened -> Unit
            is OpenResult.Fallback -> showNotice(result.message)
            is OpenResult.Failed -> showNotice(result.message)
        }
    }

    fun openDisplayMode() = openSystem(SystemDestination.DISPLAY)

    fun run(shortcut: ShortcutUi) {
        if (!shortcut.available) {
            showNotice("${shortcut.shortcut.label} is unavailable on this TV. Hold Select to edit or remove it.")
            return
        }
        when (val a = shortcut.shortcut.action) {
            is ShortcutAction.OpenApp -> shortcut.app?.let(::launch)
            is ShortcutAction.OpenSystem -> openSystem(a.destination)
            is ShortcutAction.OpenRomi -> openRomiTarget(a.target)
        }
    }

    // Shortcuts

    fun addShortcut(action: ShortcutAction, label: String, iconKey: String) {
        val shortcut = Shortcut(UUID.randomUUID().toString(), label, iconKey, action)
        settingsStore.setShortcuts(settingsStore.shortcuts.value + shortcut)
        showNotice("Added $label to the sidebar")
    }

    fun renameShortcut(id: String, label: String) = editShortcut(id) { it.copy(label = label.trim().ifEmpty { it.label }) }

    fun setShortcutIcon(id: String, iconKey: String) = editShortcut(id) { it.copy(iconKey = iconKey) }

    private fun editShortcut(id: String, change: (Shortcut) -> Shortcut) =
        settingsStore.setShortcuts(settingsStore.shortcuts.value.map { if (it.id == id) change(it) else it })

    fun moveShortcut(id: String, delta: Int) {
        val list = settingsStore.shortcuts.value.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        settingsStore.setShortcuts(list)
    }

    fun removeShortcut(id: String) =
        settingsStore.setShortcuts(settingsStore.shortcuts.value.filterNot { it.id == id })

    fun restoreDefaultShortcuts() {
        settingsStore.restoreDefaultShortcuts()
        showNotice("Default shortcuts restored")
    }

    // Settings

    fun update(change: (RomiSettings) -> RomiSettings) = settingsStore.update(change)

    fun restoreDefaultLayout() {
        settingsStore.restoreDefaultLayout()
        showNotice("Home layout restored")
    }

    fun confirm(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit) {
        dialog = DialogState.Confirm(title, message, confirmLabel, onConfirm)
    }

    suspend fun importWallpaper(uri: Uri) {
        if (container.wallpaper.import(uri)) {
            update { it.copy(background = com.example.romilauncher.data.BackgroundId.CUSTOM, wallpaperVersion = System.currentTimeMillis()) }
        } else {
            showNotice("That image couldn't be used.")
        }
    }

    suspend fun loadWallpaper() = container.wallpaper.load()

    fun request(request: ActivityRequest) {
        _requests.trySend(request)
    }

    // Off timer: the TV's own settings own the real timer, so Romi only takes the user there.
    fun openOffTimer() = openSystem(SystemDestination.SLEEP_TIMER)

    companion object {
        private val PREFERRED = listOf(
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus",
            "com.wbd.stream",
            "com.apple.atve.androidtv.appletv",
            "com.spotify.tv.android",
            "org.xbmc.kodi",
            "com.plexapp.android",
        )
        private const val DEFAULT_PIN_COUNT = 8

        /** Sensible first-run pins: well-known streaming apps, then installed TV apps, user-installed first. */
        fun defaultPinned(all: List<AppEntry>): List<String> {
            val installed = all.associateBy { it.packageName }
            val known = PREFERRED.filter { it in installed }
            val rest = all.filter { it.isLeanback && it.packageName !in known }
                .sortedBy { it.isSystem }
                .map { it.packageName }
            return (known + rest).take(DEFAULT_PIN_COUNT)
        }
    }
}
