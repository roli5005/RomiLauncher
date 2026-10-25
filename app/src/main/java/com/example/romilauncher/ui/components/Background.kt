package com.example.romilauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import com.example.romilauncher.data.BackgroundId
import com.example.romilauncher.ui.theme.Romi

@Composable
fun RomiBackground(loadWallpaper: suspend () -> ImageBitmap?, modifier: Modifier = Modifier) {
    val colors = Romi.colors
    val settings = Romi.settings
    Box(modifier.fillMaxSize().background(colors.background)) {
        when (settings.background) {
            BackgroundId.SOLID -> Unit
            BackgroundId.WARM_GLOW -> Glow(Color(0xFFB8641E), Offset(0f, 1f), 0.20f)
            BackgroundId.COOL_GLOW -> Glow(colors.accent, Offset(1f, 0f), 0.16f)
            BackgroundId.CUSTOM -> {
                val image by produceState<ImageBitmap?>(null, settings.wallpaperVersion) { value = loadWallpaper() }
                val loaded = image
                if (loaded != null) {
                    Image(
                        bitmap = loaded,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // No image picked yet, or it can't be read: show the default glow, not a blank screen.
                    Glow(colors.accent, Offset(1f, 0f), 0.16f)
                }
            }
        }
        // Dimming also keeps text readable over a bright wallpaper.
        val dim = settings.dimming / 100f
        val floor = if (settings.background == BackgroundId.CUSTOM) 0.35f else 0f
        val alpha = maxOf(dim, floor)
        if (alpha > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)))
    }
}

@Composable
private fun Glow(color: Color, corner: Offset, strength: Float) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(color.copy(alpha = strength), Color.Transparent),
                center = Offset(corner.x * 1600f, corner.y * 900f),
                radius = 1100f,
            ),
        ),
    )
}
