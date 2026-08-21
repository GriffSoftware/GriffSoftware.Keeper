package com.griff.keeper.presentation.datatransfer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/** Which of the three data-transfer actions a card offers. */
internal enum class DataTransferAction {
    EXPORT,
    SHARE,
    IMPORT,
}

/**
 * One of the three things the user can do with their data.
 *
 * A card rather than a list row: each one is a distinct operation with consequences, and the extra
 * line of explanation is part of the decision rather than a subtitle. The icons follow the direction
 * of the data - up and out, off to someone else, down and in - so the three are told apart before
 * their labels are read.
 */
@Composable
internal fun DataTransferActionCard(
    action: DataTransferAction,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ActionIconSize),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Text(
                    text = stringResource(action.titleRes()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(action.descriptionRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The two claims the app is willing to make about backups, stated without embellishment.
 *
 * Encryption and locality are the properties a user has to be able to rely on, so they are on the
 * screen rather than in a privacy page. The wording stays factual on purpose: "encrypted, and the
 * password is required" is verifiable, while "military grade" or "impossible to break" would be
 * marketing dressed as a security guarantee.
 */
@Composable
internal fun DataSecurityCard(modifier: Modifier = Modifier) {
    InformationCard(
        icon = Icons.Default.Lock,
        title = stringResource(R.string.data_transfer_security_title),
        description = stringResource(R.string.data_transfer_security_description),
        modifier = modifier,
    )
}

@Composable
internal fun DataLocalityCard(modifier: Modifier = Modifier) {
    InformationCard(
        icon = Icons.Default.HomeWork,
        title = stringResource(R.string.data_transfer_locality_title),
        description = stringResource(R.string.data_transfer_locality_description),
        modifier = modifier,
    )
}

/**
 * Shown when nothing on the device can take a share intent.
 *
 * Not a snackbar: the answer comes with an alternative the user may want a moment to consider, and
 * an offer that disappears on its own is not much of an offer.
 */
@Composable
internal fun ShareUnavailableCard(
    onSaveToFile: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = GriffTheme.colors.warning,
                    modifier = Modifier.size(BannerIconSize),
                )
                Text(
                    text = stringResource(R.string.data_transfer_no_share_target_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = GriffTheme.colors.warning,
                )
            }
            Text(
                text = stringResource(R.string.data_transfer_no_share_target_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = onSaveToFile) {
                    Text(stringResource(R.string.data_transfer_no_share_target_action))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.data_transfer_no_share_target_dismiss))
                }
            }
        }
    }
}

@Composable
private fun InformationCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(BannerIconSize),
                )
                Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun DataTransferAction.icon(): ImageVector = when (this) {
    DataTransferAction.EXPORT -> Icons.Default.FileUpload
    DataTransferAction.SHARE -> Icons.AutoMirrored.Filled.Send
    DataTransferAction.IMPORT -> Icons.Default.FileDownload
}

private fun DataTransferAction.titleRes(): Int = when (this) {
    DataTransferAction.EXPORT -> R.string.data_transfer_export_title
    DataTransferAction.SHARE -> R.string.data_transfer_share_title
    DataTransferAction.IMPORT -> R.string.data_transfer_import_title
}

private fun DataTransferAction.descriptionRes(): Int = when (this) {
    DataTransferAction.EXPORT -> R.string.data_transfer_export_description
    DataTransferAction.SHARE -> R.string.data_transfer_share_description
    DataTransferAction.IMPORT -> R.string.data_transfer_import_description
}

private val ActionIconSize = 24.dp
private val BannerIconSize = 18.dp

@ThemePreviews
@Composable
private fun DataTransferCardsPreview() {
    GriffThemePreview {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            DataTransferActionCard(DataTransferAction.EXPORT, enabled = true, onClick = {})
            DataTransferActionCard(DataTransferAction.SHARE, enabled = true, onClick = {})
            DataTransferActionCard(DataTransferAction.IMPORT, enabled = true, onClick = {})
            DataSecurityCard()
            DataLocalityCard()
        }
    }
}
