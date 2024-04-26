package com.flexa.identity.create_id

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import com.flexa.identity.Keyboard
import com.flexa.identity.keyboardAsState

@Composable
fun KeyboardHandler() {
    val keyboardState by keyboardAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(keyboardState) {
        if (keyboardState == Keyboard.CLOSED)
            focusManager.clearFocus()
    }
}

@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    boxScope: BoxScope,
    icon: ImageVector = Icons.Rounded.ArrowBack,
    onClick: () -> Unit
) {
    boxScope.run {
        IconButton(
            modifier = modifier,
            onClick = { onClick.invoke() }) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
