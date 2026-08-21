package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.ResolvedMessage
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.Spacing

/**
 * Snackbar host that shows the severity of a message.
 *
 * The snackbar itself stays a neutral surface - only the leading icon carries the status color, and
 * it always comes with a shape and a spoken label, so severity never depends on color alone.
 */
@Composable
internal fun GriffSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val severity = (data.visuals as? MessageSnackbarVisuals)?.severity ?: MessageSeverity.INFO

        Snackbar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = severity.icon(),
                    contentDescription = stringResource(severity.labelRes()),
                    tint = severity.tint(),
                    modifier = Modifier.size(SnackbarIconSize),
                )
                Text(
                    text = data.visuals.message,
                    modifier = Modifier.padding(start = Spacing.Medium),
                )
            }
        }
    }
}

/** Shows [message] so that [GriffSnackbarHost] can pick the icon and the status color. */
internal suspend fun SnackbarHostState.showMessage(message: ResolvedMessage) {
    showSnackbar(MessageSnackbarVisuals(message = message.text, severity = message.severity))
}

private class MessageSnackbarVisuals(
    override val message: String,
    val severity: MessageSeverity,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

private fun MessageSeverity.icon(): ImageVector = when (this) {
    MessageSeverity.INFO -> Icons.Default.Info
    MessageSeverity.SUCCESS -> Icons.Default.CheckCircle
    MessageSeverity.WARNING -> Icons.Default.WarningAmber
    MessageSeverity.ERROR -> Icons.Default.ErrorOutline
}

private fun MessageSeverity.labelRes(): Int = when (this) {
    MessageSeverity.INFO -> R.string.severity_info
    MessageSeverity.SUCCESS -> R.string.severity_success
    MessageSeverity.WARNING -> R.string.severity_warning
    MessageSeverity.ERROR -> R.string.severity_error
}

@Composable
private fun MessageSeverity.tint(): Color = when (this) {
    MessageSeverity.INFO -> GriffTheme.colors.info
    MessageSeverity.SUCCESS -> GriffTheme.colors.success
    MessageSeverity.WARNING -> GriffTheme.colors.warning
    MessageSeverity.ERROR -> MaterialTheme.colorScheme.error
}

private val SnackbarIconSize = 20.dp
