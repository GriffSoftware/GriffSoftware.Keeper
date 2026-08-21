package com.griff.keeper.presentation.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/** Destinations reachable from the navigation drawer. */
enum class DrawerDestination {
    SUBSCRIPTIONS,
    OBLIGATIONS,
    STATISTICS,
    REMINDERS,
    DATA_TRANSFER,
    ABOUT,
}

/**
 * Drawer content: app identity, primary destinations and the real build information.
 *
 * The selected destination uses the primary container rather than the Material default (the neutral
 * secondary container), because navigation selection is one of the few places where the brand accent
 * carries meaning. Its indicator is nearly square instead of the Material pill, which suits the
 * app's technical look.
 */
@Composable
internal fun AppDrawerContent(
    selected: DrawerDestination,
    appVersion: AppVersion?,
    onSelect: (DrawerDestination) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.ExtraLarge),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    Text(
                        text = stringResource(R.string.app_display_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_griff_emblem),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = Spacing.Medium)
                        .height(HeaderEmblemHeight),
                )
            }

            val itemShape = RoundedCornerShape(SelectionCornerRadius)
            val itemColors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_subscriptions)) },
                icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
                selected = selected == DrawerDestination.SUBSCRIPTIONS,
                onClick = { onSelect(DrawerDestination.SUBSCRIPTIONS) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                shape = itemShape,
                colors = itemColors,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_obligations)) },
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                selected = selected == DrawerDestination.OBLIGATIONS,
                onClick = { onSelect(DrawerDestination.OBLIGATIONS) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                shape = itemShape,
                colors = itemColors,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_statistics)) },
                icon = { Icon(Icons.Default.InsertChartOutlined, contentDescription = null) },
                selected = selected == DrawerDestination.STATISTICS,
                onClick = { onSelect(DrawerDestination.STATISTICS) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                shape = itemShape,
                colors = itemColors,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_reminders)) },
                icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                selected = selected == DrawerDestination.REMINDERS,
                onClick = { onSelect(DrawerDestination.REMINDERS) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                shape = itemShape,
                colors = itemColors,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_data_transfer)) },
                icon = { Icon(Icons.Default.ImportExport, contentDescription = null) },
                selected = selected == DrawerDestination.DATA_TRANSFER,
                onClick = { onSelect(DrawerDestination.DATA_TRANSFER) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                shape = itemShape,
                colors = itemColors,
            )
            // Last in the list: not a place the user works, but where the app explains itself.
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_about)) },
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                selected = selected == DrawerDestination.ABOUT,
                onClick = { onSelect(DrawerDestination.ABOUT) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                shape = itemShape,
                colors = itemColors,
            )

            Spacer(Modifier.weight(1f))

            HorizontalDivider()

            Column(
                modifier = Modifier.padding(
                    horizontal = Spacing.ExtraLarge,
                    vertical = Spacing.Large,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = stringResource(R.string.drawer_version, appVersion?.name ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.drawer_build, appVersion?.code ?: 0L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Shrunk to sit comfortably next to the two-line title block without dominating the header; the
 * emblem's own aspect ratio (from [R.drawable.ic_griff_emblem]) determines its width.
 */
private val HeaderEmblemHeight = 80.dp

/** Material would draw a full pill here; the brand asks for a corner that is barely rounded. */
private val SelectionCornerRadius = 4.dp

@ThemePreviews
@Composable
private fun AppDrawerContentPreview() {
    GriffThemePreview {
        AppDrawerContent(
            selected = DrawerDestination.SUBSCRIPTIONS,
            appVersion = AppVersion(name = "1.0.0", code = 1L),
            onSelect = {},
        )
    }
}
