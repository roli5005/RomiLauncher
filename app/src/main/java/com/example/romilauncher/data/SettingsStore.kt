package com.example.romilauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistence for every user preference. Reads tolerate unknown or missing values, so a newer or
 * older build never crashes on stored data and falls back to defaults instead.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("romi_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<RomiSettings> = _settings.asStateFlow()

    /** Pinned package names in order, or null while the user has never customised them (defaults apply). */
    private val _pinned = MutableStateFlow(readPinned())
    val pinned: StateFlow<List<String>?> = _pinned.asStateFlow()

    private val _shortcuts = MutableStateFlow(readShortcuts())
    val shortcuts: StateFlow<List<Shortcut>> = _shortcuts.asStateFlow()

    fun update(transform: (RomiSettings) -> RomiSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        writeSettings(next)
    }

    fun setPinned(packages: List<String>) {
        val distinct = packages.distinct()
        _pinned.value = distinct
        prefs.edit { putString(KEY_PINNED, JSONArray(distinct).toString()) }
    }

    fun setShortcuts(shortcuts: List<Shortcut>) {
        _shortcuts.value = shortcuts
        prefs.edit { putString(KEY_SHORTCUTS, shortcuts.toJson().toString()) }
    }

    fun restoreDefaultShortcuts() {
        _shortcuts.value = defaultShortcuts()
        prefs.edit { remove(KEY_SHORTCUTS) }
    }

    /** Home-screen layout back to factory state. Shortcuts and appearance are left alone. */
    fun restoreDefaultLayout() {
        _pinned.value = null
        prefs.edit { remove(KEY_PINNED) }
        val defaults = RomiSettings()
        update {
            it.copy(
                showApps = defaults.showApps,
                showContinueWatching = defaults.showContinueWatching,
                showTrending = defaults.showTrending,
                density = defaults.density,
                appSort = defaults.appSort,
            )
        }
    }

    private fun readSettings(): RomiSettings {
        val d = RomiSettings()
        return RomiSettings(
            theme = prefs.enum("theme", d.theme),
            accent = prefs.enum("accent", d.accent),
            background = prefs.enum("background", d.background),
            wallpaperVersion = prefs.getLong("wallpaper_version", d.wallpaperVersion),
            dimming = prefs.getInt("dimming", d.dimming).coerceIn(0, 60),
            highContrast = prefs.getBoolean("high_contrast", d.highContrast),
            density = prefs.enum("density", d.density),
            animations = prefs.enum("animations", d.animations),
            showApps = prefs.getBoolean("show_apps", d.showApps),
            showContinueWatching = prefs.getBoolean("show_continue", d.showContinueWatching),
            showTrending = prefs.getBoolean("show_trending", d.showTrending),
            appSort = prefs.enum("app_sort", d.appSort),
            defaultFocus = prefs.enum("default_focus", d.defaultFocus),
            sidebarMode = prefs.enum("sidebar_mode", d.sidebarMode),
            focusStyle = prefs.enum("focus_style", d.focusStyle),
            longPress = prefs.enum("long_press", d.longPress),
            clockFormat = prefs.enum("clock_format", d.clockFormat),
        )
    }

    private fun writeSettings(s: RomiSettings) = prefs.edit {
        putString("theme", s.theme.name)
        putString("accent", s.accent.name)
        putString("background", s.background.name)
        putLong("wallpaper_version", s.wallpaperVersion)
        putInt("dimming", s.dimming)
        putBoolean("high_contrast", s.highContrast)
        putString("density", s.density.name)
        putString("animations", s.animations.name)
        putBoolean("show_apps", s.showApps)
        putBoolean("show_continue", s.showContinueWatching)
        putBoolean("show_trending", s.showTrending)
        putString("app_sort", s.appSort.name)
        putString("default_focus", s.defaultFocus.name)
        putString("sidebar_mode", s.sidebarMode.name)
        putString("focus_style", s.focusStyle.name)
        putString("long_press", s.longPress.name)
        putString("clock_format", s.clockFormat.name)
    }

    private fun readPinned(): List<String>? {
        val raw = prefs.getString(KEY_PINNED, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        }.getOrNull()
    }

    private fun readShortcuts(): List<Shortcut> {
        val raw = prefs.getString(KEY_SHORTCUTS, null) ?: return defaultShortcuts()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { shortcutFromJson(array.getJSONObject(it)) }
        }.getOrElse { defaultShortcuts() }
    }

    private companion object {
        const val KEY_PINNED = "pinned"
        const val KEY_SHORTCUTS = "shortcuts"
    }
}

private inline fun <reified E : Enum<E>> SharedPreferences.enum(key: String, default: E): E {
    val name = getString(key, null) ?: return default
    return enumValues<E>().firstOrNull { it.name == name } ?: default
}

private fun List<Shortcut>.toJson(): JSONArray = JSONArray().also { array ->
    forEach { s ->
        val (type, target) = when (val a = s.action) {
            is ShortcutAction.OpenApp -> "app" to a.packageName
            is ShortcutAction.OpenSystem -> "system" to a.destination.name
            is ShortcutAction.OpenRomi -> "romi" to a.target.name
        }
        array.put(
            JSONObject()
                .put("id", s.id)
                .put("label", s.label)
                .put("icon", s.iconKey)
                .put("type", type)
                .put("target", target),
        )
    }
}

private fun shortcutFromJson(o: JSONObject): Shortcut? {
    val target = o.optString("target")
    val action = when (o.optString("type")) {
        "app" -> ShortcutAction.OpenApp(target)
        "system" -> SystemDestination.entries.firstOrNull { it.name == target }?.let { ShortcutAction.OpenSystem(it) }
        "romi" -> RomiTarget.entries.firstOrNull { it.name == target }?.let { ShortcutAction.OpenRomi(it) }
        else -> null
    } ?: return null
    val id = o.optString("id").ifEmpty { return null }
    return Shortcut(id, o.optString("label", "Shortcut"), o.optString("icon", "star"), action)
}
