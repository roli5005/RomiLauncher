package com.example.romilauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import com.example.romilauncher.data.MediaItem
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiType

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = RomiType.section,
        color = Romi.colors.textPrimary,
        modifier = modifier.padding(bottom = 12.dp),
    )
}

/** Artwork card with title, optional resume text and a progress bar. Falls back to a title card when artwork fails. */
@Composable
fun MediaCard(
    item: MediaItem,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Romi.colors
    val height = width * 9 / 16
    val description = buildString {
        append(item.title)
        item.resumeInfo?.let { append(", $it") }
        item.progress?.let { append(", ${(it * 100).toInt()} percent watched") }
    }
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        description = description,
        modifier = modifier.size(width, height),
    ) {
        SubcomposeAsyncImage(
            model = item.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { Box(Modifier.fillMaxSize().background(colors.card)) },
            error = {
                Box(Modifier.fillMaxSize().background(colors.card), contentAlignment = Alignment.Center) {
                    Text(
                        item.title,
                        style = RomiType.bodyStrong,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            },
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    item.title, style = RomiType.label, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                item.resumeInfo?.let { Text(it, style = RomiType.label, color = Color(0xFFD0D5DB), maxLines = 1) }
            }
            item.progress?.let { progress ->
                Box(
                    Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.3f)),
                ) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(Color.White))
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = RomiIcons.info,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RomiIcon(icon, size = 40.dp, tint = Romi.colors.textSecondary)
        Text(title, style = RomiType.section, color = Romi.colors.textPrimary)
        Text(
            message,
            style = RomiType.body,
            color = Romi.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(420.dp),
        )
    }
}
