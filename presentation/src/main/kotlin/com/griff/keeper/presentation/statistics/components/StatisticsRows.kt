package com.griff.keeper.presentation.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.statistics.ExpenseSource
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.EntryRow
import com.griff.keeper.presentation.common.component.ProviderLogo
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.statistics.RankedExpenseItem
import com.griff.keeper.presentation.statistics.RankedSubscription
import com.griff.keeper.presentation.statistics.UpcomingCharge
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.LocalDate

/** Row of the subscription "largest costs" ranking, always compared per month. */
@Composable
internal fun RankedSubscriptionRow(
    subscription: RankedSubscription,
    modifier: Modifier = Modifier,
) {
    EntryRow(
        title = subscription.name,
        amount = stringResource(
            R.string.amount_per_month,
            MoneyFormatter.format(subscription.monthlyEquivalent),
        ),
        modifier = modifier,
        verticalPadding = Spacing.Small,
        leading = {
            ProviderLogo(
                logoKey = subscription.logoKey,
                name = subscription.name,
                size = RowLogoSize,
            )
        },
    )
}

/**
 * Row of the combined ranking.
 *
 * The source is spelled out under the name, because a subscription's monthly cost and an
 * obligation's paid amount are not the same kind of number and the list must not imply they are.
 */
@Composable
internal fun RankedExpenseRow(
    expense: RankedExpenseItem,
    modifier: Modifier = Modifier,
) {
    EntryRow(
        title = expense.name,
        amount = when (expense.source) {
            ExpenseSource.SUBSCRIPTION -> stringResource(
                R.string.amount_per_month,
                MoneyFormatter.format(expense.amount),
            )

            ExpenseSource.OBLIGATION -> MoneyFormatter.format(expense.amount)
        },
        modifier = modifier,
        verticalPadding = Spacing.Small,
        supporting = {
            Text(
                text = stringResource(expense.sourceLabelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

/**
 * Row of an expected charge, showing the date it is due.
 *
 * A charge that is about to happen is marked with a warning-tinted badge. The badge is labelled, so
 * the warning survives being read out loud or seen without color perception.
 */
@Composable
internal fun UpcomingChargeRow(
    charge: UpcomingCharge,
    modifier: Modifier = Modifier,
) {
    EntryRow(
        title = charge.name,
        amount = MoneyFormatter.format(charge.amount),
        modifier = modifier,
        verticalPadding = Spacing.Small,
        leading = {
            ProviderLogo(logoKey = charge.logoKey, name = charge.name, size = RowLogoSize)
        },
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

private val RowLogoSize = 36.dp
private val BadgeCornerRadius = 6.dp
private const val BadgeBackgroundAlpha = 0.22f

@ThemePreviews
@Composable
private fun StatisticsRowsPreview() {
    GriffThemePreview {
        Column(modifier = Modifier.padding(vertical = Spacing.Large)) {
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
            RankedSubscriptionRow(
                subscription = RankedSubscription(
                    id = "3",
                    name = "Google Workspace",
                    logoKey = "google_workspace",
                    billingPeriod = BillingPeriod.YEARLY,
                    monthlyEquivalent = Money.ofUnits(86),
                ),
            )
            RankedExpenseRow(
                expense = RankedExpenseItem(
                    id = "4",
                    name = "OC Ford",
                    amount = Money.ofUnits(1_240),
                    source = ExpenseSource.OBLIGATION,
                    sourceLabelRes = R.string.statistics_source_obligation,
                ),
            )
        }
    }
}
