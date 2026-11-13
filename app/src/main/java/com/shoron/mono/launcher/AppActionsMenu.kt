package com.shoron.mono.launcher

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

data class AppAction(val label: String, val onClick: () -> Unit)

/** Long-press menu shared by the home list and the drawer. */
@Composable
fun AppActionsMenu(expanded: Boolean, onDismiss: () -> Unit, actions: List<AppAction>) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = { Text(action.label) },
                onClick = {
                    onDismiss()
                    action.onClick()
                },
            )
        }
    }
}
