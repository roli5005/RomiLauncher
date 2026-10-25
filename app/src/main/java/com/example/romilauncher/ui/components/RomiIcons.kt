package com.example.romilauncher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.example.romilauncher.ui.theme.Romi

/** The predefined icon library users pick shortcut icons from. Keys are persisted, so never rename one. */
object RomiIcons {
    val library: LinkedHashMap<String, ImageVector> = linkedMapOf(
        "star" to Icons.Rounded.Star,
        "heart" to Icons.Rounded.Favorite,
        "home" to Icons.Rounded.Home,
        "play" to Icons.Rounded.PlayArrow,
        "movie" to Icons.Rounded.Movie,
        "livetv" to Icons.Rounded.LiveTv,
        "tv" to Icons.Rounded.Tv,
        "music" to Icons.Rounded.MusicNote,
        "game" to Icons.Rounded.SportsEsports,
        "folder" to Icons.Rounded.Folder,
        "cloud" to Icons.Rounded.Cloud,
        "bolt" to Icons.Rounded.Bolt,
        "person" to Icons.Rounded.Person,
        "lock" to Icons.Rounded.Lock,
        "display" to Icons.Rounded.Monitor,
        "volume" to Icons.Rounded.VolumeUp,
        "wifi" to Icons.Rounded.Wifi,
        "network" to Icons.Rounded.Lan,
        "bluetooth" to Icons.Rounded.Bluetooth,
        "settings" to Icons.Rounded.Settings,
        "tune" to Icons.Rounded.Tune,
        "clock" to Icons.Rounded.Schedule,
        "timer" to Icons.Rounded.Timer,
        "moon" to Icons.Rounded.Bedtime,
        "power" to Icons.Rounded.PowerSettingsNew,
        "language" to Icons.Rounded.Language,
        "apps" to Icons.Rounded.Apps,
        "storage" to Icons.Rounded.Storage,
        "accessibility" to Icons.Rounded.Accessibility,
        "keyboard" to Icons.Rounded.Keyboard,
        "info" to Icons.Rounded.Info,
        "search" to Icons.Rounded.Search,
    )

    fun get(key: String): ImageVector = library[key] ?: Icons.Rounded.Star

    // Interface glyphs that are not offered to users.
    val add = Icons.Rounded.Add
    val back = Icons.Rounded.ArrowBack
    val check = Icons.Rounded.Check
    val chevron = Icons.Rounded.ChevronRight
    val delete = Icons.Rounded.Delete
    val edit = Icons.Rounded.Edit
    val palette = Icons.Rounded.Palette
    val pause = Icons.Rounded.Pause
    val pin = Icons.Rounded.PushPin
    val play = Icons.Rounded.PlayArrow
    val up = Icons.Rounded.ArrowUpward
    val down = Icons.Rounded.ArrowDownward
    val forward = Icons.Rounded.ArrowForward
    val home = Icons.Rounded.Home
    val search = Icons.Rounded.Search
    val settings = Icons.Rounded.Settings
    val timer = Icons.Rounded.Timer
    val display = Icons.Rounded.Monitor
    val apps = Icons.Rounded.Apps
    val info = Icons.Rounded.Info
}

@Composable
fun RomiIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = if (tint == Color.Unspecified) Romi.colors.textPrimary else tint,
    )
}
