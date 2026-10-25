package com.example.romilauncher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

enum class SystemCategory(val label: String) {
    DISPLAY("Display and picture"),
    SOUND("Sound"),
    NETWORK("Wi-Fi and network"),
    ANDROID("Android system"),
    POWER("Power"),
}

/**
 * Public Settings actions only. Each destination lists the actions to try in order, and the first one
 * the device can resolve wins. Whether a TV exposes a given screen varies by vendor.
 */
enum class SystemDestination(
    val title: String,
    val category: SystemCategory,
    val icon: String,
    val actions: List<String>,
    /** Shown instead of the generic message when the device opens the general settings screen. */
    val fallbackHint: String? = null,
) {
    DISPLAY("Display and picture", SystemCategory.DISPLAY, "display", listOf(Settings.ACTION_DISPLAY_SETTINGS)),
    SOUND("Sound", SystemCategory.SOUND, "volume", listOf(Settings.ACTION_SOUND_SETTINGS)),
    WIFI("Wi-Fi", SystemCategory.NETWORK, "wifi", listOf(Settings.ACTION_WIFI_SETTINGS)),
    NETWORK("Network and internet", SystemCategory.NETWORK, "network", listOf(Settings.ACTION_WIRELESS_SETTINGS)),
    BLUETOOTH("Bluetooth", SystemCategory.NETWORK, "bluetooth", listOf(Settings.ACTION_BLUETOOTH_SETTINGS)),
    ANDROID_SETTINGS("Android settings", SystemCategory.ANDROID, "settings", listOf(Settings.ACTION_SETTINGS)),
    DATE_TIME("Date and time", SystemCategory.ANDROID, "clock", listOf(Settings.ACTION_DATE_SETTINGS)),
    LANGUAGE("Language", SystemCategory.ANDROID, "language", listOf(Settings.ACTION_LOCALE_SETTINGS)),
    MANAGE_APPS(
        "Manage apps", SystemCategory.ANDROID, "apps",
        listOf(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS, Settings.ACTION_APPLICATION_SETTINGS),
    ),
    STORAGE("Storage", SystemCategory.ANDROID, "storage", listOf(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)),
    ACCESSIBILITY("Accessibility", SystemCategory.ANDROID, "accessibility", listOf(Settings.ACTION_ACCESSIBILITY_SETTINGS)),
    KEYBOARD("Keyboard", SystemCategory.ANDROID, "keyboard", listOf(Settings.ACTION_INPUT_METHOD_SETTINGS)),
    // Android has no public action for the sleep timer, so this opens the general settings screen.
    SLEEP_TIMER(
        "Off timer", SystemCategory.POWER, "timer", emptyList(),
        fallbackHint = "Opened Android settings. Go to System, then Power and Energy, then Sleep Timer.",
    ),
    SCREEN_SAVER("Screen saver", SystemCategory.POWER, "moon", listOf(Settings.ACTION_DREAM_SETTINGS)),
    BATTERY_SAVER("Battery saver", SystemCategory.POWER, "power", listOf(Settings.ACTION_BATTERY_SAVER_SETTINGS)),
}

sealed interface OpenResult {
    data object Opened : OpenResult
    data class Fallback(val message: String) : OpenResult
    data class Failed(val message: String) : OpenResult
}

object SystemLauncher {

    private fun resolve(context: Context, destination: SystemDestination): Intent? =
        destination.actions
            .map { Intent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .firstOrNull { it.resolveActivity(context.packageManager) != null }

    fun isAvailable(context: Context, destination: SystemDestination): Boolean =
        resolve(context, destination) != null

    /** Opens [destination], or the general Android settings screen when the device has no direct match. */
    fun open(context: Context, destination: SystemDestination): OpenResult {
        if (tryStart(context, resolve(context, destination))) return OpenResult.Opened
        if (destination != SystemDestination.ANDROID_SETTINGS &&
            tryStart(context, resolve(context, SystemDestination.ANDROID_SETTINGS))
        ) {
            return OpenResult.Fallback(
                destination.fallbackHint
                    ?: "${destination.title} can't be opened directly on this TV. Opened Android settings instead.",
            )
        }
        return OpenResult.Failed("${destination.title} isn't available on this TV.")
    }

    private fun tryStart(context: Context, intent: Intent?): Boolean {
        if (intent == null) return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
