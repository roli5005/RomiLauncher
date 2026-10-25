package com.example.romilauncher.data

import android.content.ComponentName

interface Labeled {
    val label: String
}

enum class ThemeId(override val label: String) : Labeled {
    CINEMATIC("Cinematic"),
    GRAPHITE("Graphite"),
    AMOLED("Pure black"),
}

enum class AccentId(override val label: String, val argb: Long) : Labeled {
    ICE("Ice blue", 0xFF8AB4FF),
    FROST("Frost white", 0xFFDCE8FF),
    TEAL("Teal", 0xFF5CD6C8),
    AMBER("Amber", 0xFFFFB95C),
    VIOLET("Violet", 0xFFB79CFF),
    ROSE("Rose", 0xFFFF8FA3),
}

enum class BackgroundId(override val label: String) : Labeled {
    SOLID("Solid"),
    WARM_GLOW("Warm glow"),
    COOL_GLOW("Cool glow"),
    CUSTOM("Custom image"),
}

enum class CardDensity(override val label: String, val scale: Float) : Labeled {
    COMPACT("Compact", 0.88f),
    COMFORTABLE("Comfortable", 1f),
    LARGE("Large", 1.15f),
}

enum class AnimationMode(override val label: String) : Labeled {
    FULL("Full"),
    REDUCED("Reduced"),
    OFF("Off"),
}

enum class DefaultFocus(override val label: String) : Labeled {
    APPS("Your Apps"),
    SIDEBAR("Sidebar"),
    TOP_BAR("Top bar"),
}

enum class SidebarMode(override val label: String) : Labeled {
    EXPANDED("Always expanded"),
    COMPACT("Icons, expand on focus"),
}

enum class FocusStyle(override val label: String) : Labeled {
    OUTLINE("Outline"),
    OUTLINE_GLOW("Outline and glow"),
}

enum class LongPressAction(override val label: String) : Labeled {
    MENU("Open menu"),
    TOGGLE_PIN("Pin or unpin"),
    NONE("Do nothing"),
}

enum class ClockFormat(override val label: String) : Labeled {
    SYSTEM("Follow system"),
    H12("12-hour"),
    H24("24-hour"),
}

enum class AppSort(override val label: String) : Labeled {
    ALPHABETICAL("A to Z"),
    FAVORITES_FIRST("Favorites first"),
}

data class RomiSettings(
    val theme: ThemeId = ThemeId.CINEMATIC,
    val accent: AccentId = AccentId.ICE,
    val background: BackgroundId = BackgroundId.WARM_GLOW,
    val wallpaperVersion: Long = 0L,
    /** 0..60, percent of black laid over the background. */
    val dimming: Int = 0,
    val highContrast: Boolean = false,
    val density: CardDensity = CardDensity.COMFORTABLE,
    val animations: AnimationMode = AnimationMode.FULL,
    val showApps: Boolean = true,
    val showContinueWatching: Boolean = true,
    val showTrending: Boolean = true,
    val appSort: AppSort = AppSort.ALPHABETICAL,
    val defaultFocus: DefaultFocus = DefaultFocus.APPS,
    val sidebarMode: SidebarMode = SidebarMode.EXPANDED,
    val focusStyle: FocusStyle = FocusStyle.OUTLINE,
    val longPress: LongPressAction = LongPressAction.MENU,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
)

/** One launchable package. Keyed by package name, so pins survive app updates. */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isLeanback: Boolean,
    val isSystem: Boolean,
    val lastUpdateTime: Long,
) {
    val component: ComponentName get() = ComponentName(packageName, activityName)
}

enum class RomiTarget(val label: String, val icon: String) {
    SETTINGS("Romi settings", "settings"),
    SLEEP_TIMER("Off timer", "timer"),
    ALL_APPS("All apps", "apps"),
    SEARCH("Search", "search"),
}

sealed interface ShortcutAction {
    data class OpenApp(val packageName: String) : ShortcutAction
    data class OpenSystem(val destination: SystemDestination) : ShortcutAction
    data class OpenRomi(val target: RomiTarget) : ShortcutAction
}

/** [iconKey] is a key of the icon library, or [APP_ICON] to show the target app's own icon. */
data class Shortcut(
    val id: String,
    val label: String,
    val iconKey: String,
    val action: ShortcutAction,
) {
    companion object {
        const val APP_ICON = "app"
    }
}

fun defaultShortcuts(): List<Shortcut> = listOf(
    Shortcut("default-sound", "Sound", "volume", ShortcutAction.OpenSystem(SystemDestination.SOUND)),
    Shortcut("default-wifi", "Wi-Fi", "wifi", ShortcutAction.OpenSystem(SystemDestination.WIFI)),
)

enum class MediaSection { CONTINUE_WATCHING, TRENDING, MOVIES, TV_SHOWS, LIBRARY }

data class MediaItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    /** 0..1, or null when the source has no progress data. */
    val progress: Float? = null,
    val resumeInfo: String? = null,
    val sourceId: String,
    /** Intent URI (see [android.content.Intent.parseUri]) that opens or resumes the item. */
    val launchUri: String? = null,
)
