package com.example.romilauncher.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.example.romilauncher.data.AccentId
import com.example.romilauncher.data.AnimationMode
import com.example.romilauncher.data.FocusStyle
import com.example.romilauncher.data.RomiSettings
import com.example.romilauncher.data.ThemeId

@Immutable
data class RomiColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardFocused: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val focusRing: Color,
    val divider: Color,
    val danger: Color = Color(0xFFFF8A80),
)

fun romiColors(theme: ThemeId, accent: AccentId, highContrast: Boolean): RomiColors {
    val base = when (theme) {
        ThemeId.CINEMATIC -> RomiColors(
            background = Color(0xFF080A0D), surface = Color(0xFF11151B),
            card = Color(0xFF191F27), cardFocused = Color(0xFF252D38),
            textPrimary = Color(0xFFF4F6F8), textSecondary = Color(0xFFA0A8B3),
            accent = Color.Unspecified, onAccent = Color(0xFF08101C),
            focusRing = Color(0xFFF4F6F8), divider = Color(0x1FFFFFFF),
        )
        ThemeId.GRAPHITE -> RomiColors(
            background = Color(0xFF0E0F11), surface = Color(0xFF17191C),
            card = Color(0xFF212428), cardFocused = Color(0xFF2E3238),
            textPrimary = Color(0xFFF2F2F0), textSecondary = Color(0xFFA6A8AB),
            accent = Color.Unspecified, onAccent = Color(0xFF101010),
            focusRing = Color(0xFFF2F2F0), divider = Color(0x1FFFFFFF),
        )
        ThemeId.AMOLED -> RomiColors(
            background = Color(0xFF000000), surface = Color(0xFF0A0B0C),
            card = Color(0xFF151618), cardFocused = Color(0xFF212327),
            textPrimary = Color(0xFFF4F6F8), textSecondary = Color(0xFF9EA5AE),
            accent = Color.Unspecified, onAccent = Color(0xFF08101C),
            focusRing = Color(0xFFF4F6F8), divider = Color(0x24FFFFFF),
        )
    }.copy(accent = Color(accent.argb))
    return if (highContrast) {
        base.copy(
            textSecondary = Color(0xFFD8DDE3),
            divider = Color(0x66FFFFFF),
            card = base.card.lighten(0.04f),
            focusRing = Color.White,
        )
    } else {
        base
    }
}

private fun Color.lighten(by: Float) = Color(
    red = (red + by).coerceAtMost(1f), green = (green + by).coerceAtMost(1f),
    blue = (blue + by).coerceAtMost(1f), alpha = alpha,
)

@Immutable
data class RomiMotion(
    /** Focus transition length. 0 means no animation at all. */
    val focusMs: Int,
    val focusScale: Float,
    val glow: Boolean,
    val strongRing: Boolean,
) {
    val animated: Boolean get() = focusMs > 0
}

fun romiMotion(settings: RomiSettings, systemAnimatorScale: Float): RomiMotion {
    // The system "remove animations" switch always wins over Romi's own preference.
    val mode = if (systemAnimatorScale == 0f) AnimationMode.OFF else settings.animations
    return when (mode) {
        AnimationMode.FULL -> RomiMotion(160, 1.06f, settings.focusStyle == FocusStyle.OUTLINE_GLOW, settings.highContrast)
        AnimationMode.REDUCED -> RomiMotion(90, 1f, settings.focusStyle == FocusStyle.OUTLINE_GLOW, settings.highContrast)
        AnimationMode.OFF -> RomiMotion(0, 1f, settings.focusStyle == FocusStyle.OUTLINE_GLOW, settings.highContrast)
    }
}

object RomiType {
    private val family = FontFamily.SansSerif
    private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    val logo = TextStyle(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 30.sp, letterSpacing = 1.sp)
    val title = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeightStyle = trim)
    val section = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeightStyle = trim)
    val body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeightStyle = trim)
    val bodyStrong = body.copy(fontWeight = FontWeight.Medium)
    val label = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeightStyle = trim)
}

/** 8dp grid. */
object RomiSpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val gutter = 40.dp
    val sidebarExpanded = 184.dp
    val sidebarCompact = 72.dp
    val topBarHeight = 72.dp
}

val LocalRomiColors = staticCompositionLocalOf { romiColors(ThemeId.CINEMATIC, AccentId.ICE, false) }
val LocalRomiMotion = staticCompositionLocalOf { RomiMotion(160, 1.06f, false, false) }
val LocalRomiSettings = staticCompositionLocalOf { RomiSettings() }

object Romi {
    val colors: RomiColors @Composable @ReadOnlyComposable get() = LocalRomiColors.current
    val motion: RomiMotion @Composable @ReadOnlyComposable get() = LocalRomiMotion.current
    val settings: RomiSettings @Composable @ReadOnlyComposable get() = LocalRomiSettings.current
}

@Composable
fun RomiTheme(settings: RomiSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = romiColors(settings.theme, settings.accent, settings.highContrast)
    val animatorScale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    val motion = romiMotion(settings, animatorScale)
    val scheme = darkColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent,
        background = colors.background, onBackground = colors.textPrimary,
        surface = colors.card, onSurface = colors.textPrimary,
        surfaceVariant = colors.cardFocused, onSurfaceVariant = colors.textSecondary,
        border = colors.focusRing,
    )
    CompositionLocalProvider(
        LocalRomiColors provides colors,
        LocalRomiMotion provides motion,
        LocalRomiSettings provides settings,
    ) {
        MaterialTheme(colorScheme = scheme) {
            TvScale(content)
        }
    }
}

/**
 * Layouts are designed on a 960 x 540 dp canvas, the usual 1080p TV. On other screens the density is
 * scaled so the whole layout grows or shrinks together instead of reflowing.
 */
@Composable
private fun TvScale(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val density = LocalDensity.current
    val scale = (widthDp / DESIGN_WIDTH_DP).coerceIn(0.75f, 1.6f)
    CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
        content()
    }
}

private const val DESIGN_WIDTH_DP = 960f
