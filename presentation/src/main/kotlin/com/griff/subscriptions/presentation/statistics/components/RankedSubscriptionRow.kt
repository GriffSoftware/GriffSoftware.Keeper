package com.griff.subscriptions.presentation.statistics.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.component.ProviderLogo
import com.griff.subscriptions.presentation.common.format.DateFormatter
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.statistics.RankedSubscription
import com.griff.subscriptions.presentation.statistics.UpcomingCharge
import com.griff.subscriptions.presentation.theme.Spacing

/** Row of the "largest costs" ranking, always compared per month. */
@Composable
internal fun RankedSubscriptionRow(
    subscription: RankedSubscription,
    modifier: Modifier = Modifier,
) {
    ListRow(
        logoKey = subscription.logoKey,
        name = subscription.name,
        trailing = stringResource(
            R.string.amount_per_month,
            MoneyFormatter.format(subscription.monthlyEquivalent),
        ),
        modifier = modifier,
    )
}

/** Row of an expected charge, showing the date it is due. */
@Composable
internal fun UpcomingChargeRow(
    charge: UpcomingCharge,
    modifier: Modifier = Modifier,
) {
    ListRow(
        logoKey = charge.logoKey,
        name = charge.name,
        supporting = DateFormatter.formatFullDate(charge.date),
        trailing = MoneyFormatter.format(charge.amount),
        modifier = modifier,
    )
}

@Composable
private fun ListRow(
    logoKey: String,
    name: String,
    trailing: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = RowMinHeight)
            .padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderLogo(logoKey = logoKey, name = name, size = RowLogoSize)
        Spacer(Modifier.width(Spacing.Medium))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = trailing,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

private val RowLogoSize = 36.dp
private val RowMinHeight = 48.dp
