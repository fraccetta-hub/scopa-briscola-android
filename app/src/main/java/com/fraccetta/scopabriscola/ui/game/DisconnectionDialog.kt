package com.fraccetta.scopabriscola.ui.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fraccetta.scopabriscola.R

@Composable
fun DisconnectionDialog(
    onRetry: () -> Unit,
    onAbandon: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},  // non-dismissable
        title = { Text(stringResource(R.string.disconnection_title)) },
        text = { Text(stringResource(R.string.disconnection_message)) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.disconnection_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onAbandon) {
                Text(stringResource(R.string.disconnection_abandon))
            }
        }
    )
}
