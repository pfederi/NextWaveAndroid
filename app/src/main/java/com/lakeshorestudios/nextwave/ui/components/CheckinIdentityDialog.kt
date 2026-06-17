package com.lakeshorestudios.nextwave.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reusable "How others see you" dialog for the Wave Check-in identity.
 * Used both from Settings and on the first check-in tap (when no identity is set yet).
 *
 * @param initialName pre-fills the name field (e.g. the stored name or the device name)
 * @param initialAnonymous pre-selects the anonymous toggle
 * @param onSave called with the chosen (name, anonymous) when the user confirms
 * @param onDismiss called when the dialog is cancelled/dismissed
 */
@Composable
fun CheckinIdentityDialog(
    initialName: String,
    initialAnonymous: Boolean,
    onSave: (name: String, anonymous: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    var anonInput by remember { mutableStateOf(initialAnonymous) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How others see you") },
        text = {
            Column {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your name") },
                    enabled = !anonInput,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = anonInput,
                        onCheckedChange = { anonInput = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Join anonymously")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your name is visible to other foilers on this wave. Choose anonymous to be counted without a name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = anonInput || nameInput.trim().isNotEmpty(),
                onClick = { onSave(nameInput.trim(), anonInput) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
