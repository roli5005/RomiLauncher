package com.example.romilauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.romilauncher.data.AppEntry
import com.example.romilauncher.data.AppImage
import com.example.romilauncher.data.AppImageLoader
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiType

val LocalAppImageLoader = compositionLocalOf<AppImageLoader> { error("AppImageLoader not provided") }

@Composable
fun rememberAppImage(app: AppEntry): State<AppImage?> {
    val loader = LocalAppImageLoader.current
    return produceState(loader.peek(app), app.packageName, app.lastUpdateTime) { value = loader.load(app) }
}

/** 16:9 app card with the app name underneath. Uses the app's TV banner, or its icon when it has none. */
@Composable
fun AppTile(
    app: AppEntry,
    width: Dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    showPinBadge: Boolean = false,
) {
    val colors = Romi.colors
    val height = width * 9 / 16
    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FocusSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            shape = RoundedCornerShape(12.dp),
            description = if (pinned) "${app.label}, pinned" else app.label,
            modifier = modifier.size(width, height),
        ) {
            val image = rememberAppImage(app).value
            when {
                image == null -> Unit
                image.isBanner -> Image(
                    bitmap = image.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Image(
                    bitmap = image.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.align(Alignment.Center).size(height * 0.62f),
                )
            }
            if (showPinBadge && pinned) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp)
                        .clip(CircleShape).background(colors.background.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) { RomiIcon(RomiIcons.pin, size = 12.dp, tint = colors.accent) }
            }
        }
        Text(
            app.label,
            style = RomiType.label,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().height(20.dp),
        )
    }
}

@Composable
fun AllAppsTile(width: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val height = width * 9 / 16
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FocusSurface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            description = "All Apps",
            modifier = modifier.size(width, height),
        ) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RomiIcon(RomiIcons.apps, size = 28.dp, tint = Romi.colors.textPrimary)
                Text("All Apps", style = RomiType.label)
            }
        }
    }
}

/** The app's own icon at a small fixed size, for lists. */
@Composable
fun AppIconImage(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val image = rememberAppImage(app).value
    // Banners are 16:9, so they get a wider slot and are shown whole rather than cropped square.
    val width = if (image?.isBanner == true) size * 16 / 9 else size
    Box(modifier.size(width, size), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width, size).clip(RoundedCornerShape(if (image.isBanner) 6.dp else 0.dp)),
            )
        }
    }
}
