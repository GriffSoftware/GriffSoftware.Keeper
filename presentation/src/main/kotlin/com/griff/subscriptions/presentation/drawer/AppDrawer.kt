package com.griff.subscriptions.presentation.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.griff.subscriptions.application.appinfo.AppVersion
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.theme.Spacing

/** Destinations reachable from the navigation drawer. */
enum class DrawerDestination {
    HOME,
    STATISTICS,
}

/** Drawer content: app identity, primary destinations and the real build information. */
@Composable
internal fun AppDrawerContent(
    selected: DrawerDestination,
    appVersion: AppVersion?,
    onSelect: (DrawerDestination) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Spacing.ExtraLarge,
                    vertical = Spacing.ExtraLarge,
                ),
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

            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_home)) },
                icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
                selected = selected == DrawerDestination.HOME,
                onClick = { onSelect(DrawerDestination.HOME) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_statistics)) },
                icon = { Icon(Icons.Default.InsertChartOutlined, contentDescription = null) },
                selected = selected == DrawerDestination.STATISTICS,
                onClick = { onSelect(DrawerDestination.STATISTICS) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
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
