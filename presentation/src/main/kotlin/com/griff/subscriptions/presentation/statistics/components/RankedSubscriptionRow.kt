package com.griff.subscriptions.presentation.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.component.ProviderLogo
import com.griff.subscriptions.presentation.common.format.DateFormatter
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.statistics.RankedSubscription
import com.griff.subscriptions.presentation.statistics.UpcomingCharge
import com.griff.subscriptions.presentation.theme.GriffTheme
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews
import java.time.LocalDate

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

/**
 * Row of an expected charge, showing the date it is due.
 *
 * A charge that is about to happen is marked with a warning-tinted badge. The badge is labelled,
 * so the warning survives being read out loud or seen without color perception.
 */
@Composable
internal fun UpcomingChargeRow(
    charge: UpcomingCharge,
    modifier: Modifier = Modifier,
) {
    ListRow(
        logoKey = charge.logoKey,
        name = charge.name,
        trailing = MoneyFormatter.format(charge.amount),
        modifier = modifier,
        supporting = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = DateFormatter.formatFullDate(charge.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (charge.isDueSoon) DueSoonBadge()
            }
        },
    )
}

@Composable
private fun DueSoonBadge() {
    Text(
        text = stringResource(R.string.statistics_upcoming_soon),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(
                color = GriffTheme.colors.warning.copy(alpha = BadgeBackgroundAlpha),
                shape = RoundedCornerShape(BadgeCornerRadius),
            )
            .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall / 2),
    )
}

@Composable
private fun ListRow(
    logoKey: String,
    name: String,
    trailing: String,
    modifier: Modifier = Modifier,
    supporting: (@Composable () -> Unit)? = null,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.invoke()
        }
        Text(
            text = trailing,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

private val RowLogoSize = 36.dp
private val RowMinHeight = 48.dp
private val BadgeCornerRadius = 6.dp
private const val BadgeBackgroundAlpha = 0.22f

@ThemePreviews
@Composable
private fun StatisticsRowsPreview() {
    GriffThemePreview {
        Column(modifier = Modifier.padding(Spacing.Large)) {
            UpcomingChargeRow(
                charge = UpcomingCharge(
                    subscriptionId = "1",
                    name = "Spotify",
                    logoKey = "spotify",
                    date = LocalDate.of(2026, 8, 25),
                    amount = Money.ofUnits(34, 99),
                    isDueSoon = true,
                ),
            )
            UpcomingChargeRow(
                charge = UpcomingCharge(
                    subscriptionId = "2",
                    name = "Netflix",
                    logoKey = "netflix",
                    date = LocalDate.of(2026, 9, 12),
                    amount = Money.ofUnits(67),
                ),
            )
            RankedSubscriptionRow(
                subscription = RankedSubscription(
                    id = "3",
                    name = "Google Workspace",
                    logoKey = "google_workspace",
                    billingPeriod = BillingPeriod.YEARLY,
                    monthlyEquivalent = Money.ofUnits(86),
                ),
            )
        }
    }
}
