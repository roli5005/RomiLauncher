package com.example.romilauncher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.tv.material3.Text
import com.example.romilauncher.data.ClockFormat
import com.example.romilauncher.ui.HomeTab
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing
import com.example.romilauncher.ui.theme.RomiType
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TopBar(
    logoWidth: androidx.compose.ui.unit.Dp,
    activeTab: HomeTab?,
    onTab: (HomeTab) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    tabRequester: FocusRequester? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(RomiSpacing.topBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(logoWidth).padding(start = RomiSpacing.l), contentAlignment = Alignment.CenterStart) {
            Text(
                "romi",
                // The compact sidebar column is too narrow for the full-size wordmark.
                style = if (logoWidth < 150.dp) RomiType.logo.copy(fontSize = 22.sp) else RomiType.logo,
                color = Romi.colors.textPrimary,
                softWrap = false,
                maxLines = 1,
                modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true).clearAndSetSemantics { },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RomiSpacing.s), verticalAlignment = Alignment.CenterVertically) {
            HomeTab.entries.forEach { tab ->
                val selected = tab == activeTab
                FocusSurface(
                    onClick = { onTab(tab) },
                    shape = CircleShape,
                    container = if (selected) Romi.colors.card else Color.Transparent,
                    contentColor = if (selected) Romi.colors.textPrimary else Romi.colors.textSecondary,
                    description = if (selected) "${tab.label}, selected" else tab.label,
                    modifier = if (tab == HomeTab.HOME && tabRequester != null) {
                        Modifier.homeFocus("top:home", tabRequester)
                    } else {
                        Modifier.homeFocus("top:${tab.name}")
                    },
                ) {
                    Text(
                        tab.label,
                        style = if (selected) RomiType.bodyStrong else RomiType.body,
                        modifier = Modifier.padding(horizontal = RomiSpacing.m, vertical = 10.dp),
                    )
                }
            }
        }
        Box(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(RomiSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = RomiSpacing.gutter),
        ) {
            IconButton(RomiIcons.search, "Search", onSearch, Modifier.homeFocus("top:search"))
            IconButton(RomiIcons.settings, "Settings", onSettings, Modifier.homeFocus("top:settings"))
            Clock(Modifier.padding(start = RomiSpacing.s))
        }
    }
}

@Composable
fun IconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        container = Color.Transparent,
        contentColor = Romi.colors.textSecondary,
        description = description,
        modifier = modifier.size(44.dp),
    ) {
        RomiIcon(icon, Modifier.align(Alignment.Center), size = 24.dp, tint = Color.Unspecified)
    }
}

@Composable
fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val format = Romi.settings.clockFormat
    var now by remember { mutableStateOf(ZonedDateTime.now(ZoneId.systemDefault())) }

    // The system pings every minute, and again on a manual time or time-zone change.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                now = ZonedDateTime.now(ZoneId.systemDefault())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = ZonedDateTime.now(ZoneId.systemDefault()) }

    val text = formatClock(context, now, format)
    Text(
        text,
        style = RomiType.bodyStrong,
        color = Romi.colors.textSecondary,
        modifier = modifier,
    )
}

fun formatClock(context: Context, time: ZonedDateTime, format: ClockFormat): String {
    val locale = Locale.getDefault()
    val use24 = when (format) {
        ClockFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
    }
    val pattern = DateFormat.getBestDateTimePattern(locale, if (use24) "Hm" else "hm")
    return DateTimeFormatter.ofPattern(pattern, locale).format(time)
}
