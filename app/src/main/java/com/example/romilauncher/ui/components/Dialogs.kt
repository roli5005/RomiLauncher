package com.example.romilauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.example.romilauncher.data.AppEntry
import com.example.romilauncher.data.Shortcut
import com.example.romilauncher.data.ShortcutAction
import com.example.romilauncher.data.SystemCategory
import com.example.romilauncher.data.SystemDestination
import com.example.romilauncher.data.RomiTarget
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiSpacing
import com.example.romilauncher.ui.theme.RomiType

@Composable
fun RomiDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 440.dp,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Romi.colors
    val shape = RoundedCornerShape(20.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Column(
                modifier
                    .width(width)
                    .background(colors.surface, shape)
                    .border(BorderStroke(1.dp, colors.divider), shape)
                    .padding(RomiSpacing.l)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(RomiSpacing.s),
            ) {
                Text(title, style = RomiType.title.copy(fontSize = RomiType.section.fontSize), color = colors.textPrimary)
                if (subtitle != null) {
                    Text(subtitle, style = RomiType.label, color = colors.textSecondary)
                }
                Spacer(Modifier.size(RomiSpacing.xs))
                content()
            }
        }
    }
}

@Composable
fun DialogRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supporting: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = Romi.colors
    FocusSurface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        container = if (selected) colors.cardFocused else colors.card,
        contentColor = if (danger) colors.danger else colors.textPrimary,
        focusedScale = 1.02f,
        description = if (enabled) label else "$label, unavailable",
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (leading != null) leading()
            else if (icon != null) RomiIcon(icon, size = 22.dp, tint = Color.Unspecified)
            Column(Modifier.weight(1f)) {
                Text(label, style = RomiType.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (supporting != null) {
                    Text(supporting, style = RomiType.label, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (selected) RomiIcon(RomiIcons.check, size = 20.dp, tint = colors.accent)
        }
    }
}

@Composable
fun DialogButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
) {
    val colors = Romi.colors
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        container = if (primary) colors.accent else colors.card,
        focusedContainer = if (primary) colors.accent else colors.cardFocused,
        contentColor = when {
            primary -> colors.onAccent
            danger -> colors.danger
            else -> colors.textPrimary
        },
        modifier = modifier,
    ) {
        Text(
            label,
            style = RomiType.bodyStrong,
            color = if (primary) colors.onAccent else Color.Unspecified,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RomiDialog(title, onDismiss) {
        Text(message, style = RomiType.body, color = Romi.colors.textSecondary)
        Spacer(Modifier.size(RomiSpacing.s))
        Row(horizontalArrangement = Arrangement.spacedBy(RomiSpacing.s)) {
            // Safe choice holds initial focus so a stray Select press can't destroy anything.
            DialogButton("Cancel", onDismiss, Modifier.autoFocus())
            DialogButton(confirmLabel, { onConfirm(); onDismiss() }, danger = true)
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    RomiDialog(title, onDismiss, width = 480.dp) {
        RomiTextField(
            value = text,
            onValueChange = { text = it.take(24) },
            placeholder = "Name",
            modifier = Modifier.autoFocus(),
            onAction = { if (text.isNotBlank()) { onSave(text); onDismiss() } },
        )
        Spacer(Modifier.size(RomiSpacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(RomiSpacing.s)) {
            DialogButton("Save", { if (text.isNotBlank()) { onSave(text); onDismiss() } }, primary = true)
            DialogButton("Cancel", onDismiss)
        }
    }
}

@Composable
fun AppMenuDialog(
    app: AppEntry,
    pinned: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onMove: (Int) -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    RomiDialog(app.label, onDismiss, width = 380.dp) {
        DialogRow("Open", { onOpen(); onDismiss() }, Modifier.autoFocus(), icon = RomiIcons.play)
        DialogRow(
            if (pinned) "Unpin from Home" else "Pin to Home",
            { onTogglePin(); onDismiss() },
            icon = RomiIcons.pin,
        )
        // Reordering keeps the menu open so a tile can be walked several places in a row.
        if (pinned) {
            DialogRow("Move left", { onMove(-1) }, icon = RomiIcons.back, enabled = canMoveLeft)
            DialogRow("Move right", { onMove(1) }, icon = RomiIcons.forward, enabled = canMoveRight)
        }
        DialogRow("App info", { onInfo(); onDismiss() }, icon = RomiIcons.info)
        if (!app.isSystem) {
            DialogRow("Uninstall", { onUninstall(); onDismiss() }, icon = RomiIcons.delete, danger = true)
        }
    }
}

// Shortcuts

@Composable
fun ShortcutMenuDialog(
    item: com.example.romilauncher.ui.ShortcutUi,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onIcon: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    RomiDialog(
        item.shortcut.label,
        onDismiss,
        width = 380.dp,
        subtitle = if (item.available) null else "Unavailable on this TV",
    ) {
        DialogRow("Rename", { onRename() }, Modifier.autoFocus(), icon = RomiIcons.edit)
        DialogRow("Change icon", { onIcon() }, icon = RomiIcons.palette)
        DialogRow("Move up", { onMove(-1) }, icon = RomiIcons.up, enabled = canMoveUp)
        DialogRow("Move down", { onMove(1) }, icon = RomiIcons.down, enabled = canMoveDown)
        DialogRow("Remove", { onRemove() }, icon = RomiIcons.delete, danger = true)
    }
}

@Composable
fun IconPickerDialog(
    current: String,
    appIconAvailable: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Romi.colors
    val keys = remember(appIconAvailable) {
        (if (appIconAvailable) listOf(Shortcut.APP_ICON) else emptyList()) + RomiIcons.library.keys
    }
    RomiDialog("Choose an icon", onDismiss, width = 440.dp) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.heightIn(max = 260.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(keys, key = { it }) { key ->
                val selected = key == current
                FocusSurface(
                    onClick = { onPick(key) },
                    shape = RoundedCornerShape(12.dp),
                    container = if (selected) colors.cardFocused else colors.card,
                    description = if (key == Shortcut.APP_ICON) "App icon" else key,
                    modifier = Modifier
                        .size(56.dp)
                        .let { if (selected) it.autoFocus() else it },
                ) {
                    if (key == Shortcut.APP_ICON) {
                        RomiIcon(RomiIcons.apps, Modifier.align(Alignment.Center), size = 26.dp)
                    } else {
                        RomiIcon(RomiIcons.get(key), Modifier.align(Alignment.Center), size = 26.dp)
                    }
                    if (selected) {
                        RomiIcon(
                            RomiIcons.check, Modifier.align(Alignment.TopEnd).padding(3.dp),
                            size = 14.dp, tint = colors.accent,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface AddStep {
    data object Categories : AddStep
    data object Apps : AddStep
    data class System(val category: SystemCategory) : AddStep
    data object Romi : AddStep
}

@Composable
fun AddShortcutDialog(
    apps: List<AppEntry>,
    isAvailable: (SystemDestination) -> Boolean,
    onAdd: (ShortcutAction, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf<AddStep>(AddStep.Categories) }
    val title = when (val s = step) {
        AddStep.Categories -> "Add shortcut"
        AddStep.Apps -> "Installed apps"
        is AddStep.System -> s.category.label
        AddStep.Romi -> "Romi"
    }
    RomiDialog(title, onDismiss, width = 460.dp) {
        when (val s = step) {
            AddStep.Categories -> {
                DialogRow("Installed apps", { step = AddStep.Apps }, Modifier.autoFocus(), icon = RomiIcons.apps)
                SystemCategory.entries.forEach { category ->
                    DialogRow(category.label, { step = AddStep.System(category) }, icon = RomiIcons.chevron)
                }
                DialogRow("Romi settings and actions", { step = AddStep.Romi }, icon = RomiIcons.settings)
            }
            AddStep.Apps -> {
                if (apps.isEmpty()) {
                    Text("No apps found.", style = RomiType.body, color = Romi.colors.textSecondary)
                }
                LazyColumn(Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        DialogRow(
                            app.label,
                            modifier = if (index == 0) Modifier.autoFocus() else Modifier,
                            onClick =
                            { onAdd(ShortcutAction.OpenApp(app.packageName), app.label, Shortcut.APP_ICON); onDismiss() },
                            leading = { AppIconImage(app, 28.dp) },
                        )
                    }
                }
            }
            is AddStep.System -> {
                val destinations = SystemDestination.entries.filter { it.category == s.category }
                destinations.forEachIndexed { i, destination ->
                    val available = isAvailable(destination)
                    DialogRow(
                        destination.title,
                        { onAdd(ShortcutAction.OpenSystem(destination), destination.title, destination.icon); onDismiss() },
                        modifier = if (i == 0) Modifier.autoFocus() else Modifier,
                        icon = RomiIcons.get(destination.icon),
                        supporting = if (available) null else "Not available on this TV",
                        enabled = available,
                    )
                }
            }
            AddStep.Romi -> RomiTarget.entries.forEachIndexed { i, target ->
                DialogRow(
                    target.label,
                    { onAdd(ShortcutAction.OpenRomi(target), target.label, target.icon); onDismiss() },
                    modifier = if (i == 0) Modifier.autoFocus() else Modifier,
                    icon = RomiIcons.get(target.icon),
                )
            }
        }
        if (step != AddStep.Categories) {
            DialogRow("Back", { step = AddStep.Categories }, icon = RomiIcons.back)
        }
    }
}
