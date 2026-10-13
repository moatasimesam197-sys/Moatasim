package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.DestructiveActionRequest
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary

@Composable
fun DestructiveActionDialog(
    request: DestructiveActionRequest?,
    onDismiss: () -> Unit
) {
    if (request == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Safety Warning",
                tint = StageRed
            )
        },
        title = {
            Text(
                text = request.title,
                color = StageTextPrimary
            )
        },
        text = {
            Text(
                text = request.message,
                color = StageTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    request.onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StageRed,
                    contentColor = StageTextPrimary
                ),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(request.confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel", color = StageTextPrimary)
            }
        }
    )
}
