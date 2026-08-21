package com.griff.subscriptions.presentation.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.theme.Spacing

/**
 * Debug only: posts one real reminder immediately.
 *
 * Waiting a week to find out that the icon is wrong is not a workflow, and a fake preview would only
 * prove the preview works. This goes through the production publisher, so it exercises the channel,
 * the permission, the copy and the deep link at once - and it is compiled out of release builds by
 * the `BuildConfig.DEBUG` guard at the call site.
 */
@Composable
internal fun DebugToolsSection(
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Text(
            text = stringResource(R.string.reminders_debug_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onSendTestNotification, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reminders_debug_send))
        }
    }
}
