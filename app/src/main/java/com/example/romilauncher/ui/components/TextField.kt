package com.example.romilauncher.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.romilauncher.ui.theme.Romi
import com.example.romilauncher.ui.theme.RomiType

/**
 * Single-line text input for TV. Focus alone doesn't open the keyboard, so Select does, which keeps
 * D-pad navigation past the field from being hijacked by an always-open IME.
 */
@Composable
fun RomiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    imeAction: ImeAction = ImeAction.Done,
    onAction: () -> Unit = {},
) {
    val colors = Romi.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    // Select opens the keyboard only if its key-down landed here. Arriving from another control's
    // Select would otherwise deliver a stray key-up and pop the keyboard open.
    var selectDown by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(if (focused) colors.cardFocused else colors.card, shape)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) colors.focusRing else colors.divider),
                shape,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            RomiIcon(icon, size = 22.dp, tint = colors.textSecondary)
            Box(Modifier.padding(start = 12.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = RomiType.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = {
                keyboard?.hide()
                onAction()
            }),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = placeholder }
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    val code = event.key.nativeKeyCode
                    val select = code == AndroidKeyEvent.KEYCODE_DPAD_CENTER || code == AndroidKeyEvent.KEYCODE_ENTER
                    val vertical = code == AndroidKeyEvent.KEYCODE_DPAD_DOWN || code == AndroidKeyEvent.KEYCODE_DPAD_UP
                    when {
                        select && event.type == KeyEventType.KeyDown -> {
                            selectDown = true
                            false
                        }
                        select && event.type == KeyEventType.KeyUp -> {
                            val opened = selectDown
                            selectDown = false
                            if (opened) keyboard?.show()
                            opened
                        }
                        // The text field swallows Up and Down, so hand focus to the neighbours explicitly.
                        vertical && event.type == KeyEventType.KeyDown -> {
                            val direction = if (code == AndroidKeyEvent.KEYCODE_DPAD_DOWN) FocusDirection.Down else FocusDirection.Up
                            focusManager.moveFocus(direction)
                            true
                        }
                        else -> false
                    }
                },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = RomiType.body, color = colors.textSecondary)
                    }
                    inner()
                }
            },
        )
    }
}
