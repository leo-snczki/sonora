package dev.leosnczki.sonora.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraRadius
import dev.leosnczki.sonora.core.designsystem.SonoraType

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(SonoraRadius.Lg),
        containerColor = SonoraColors.SurfaceModal,
        title = { Text(title, style = SonoraType.Title, color = SonoraColors.Text) },
        text = { Text(body, style = SonoraType.Body, color = SonoraColors.TextMuted) },
        confirmButton = { SonoraButton(confirmLabel, onConfirm, variant = ButtonVariant.Text) },
        dismissButton = { SonoraButton(dismissLabel, onDismiss, variant = ButtonVariant.Text) },
    )
}

/** Snackbars replace the original's toasts. */
@Composable
fun SonoraSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(SonoraRadius.Md),
            containerColor = SonoraColors.SurfaceModal,
            contentColor = SonoraColors.Text,
            actionColor = SonoraColors.Accent,
        )
    }
}
