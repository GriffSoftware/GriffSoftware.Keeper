package com.griff.subscriptions.presentation.obligations.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScheduleSend
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Tags
import com.griff.subscriptions.presentation.common.component.EntryRow
import com.griff.subscriptions.presentation.common.component.ObligationIcon
import com.griff.subscriptions.presentation.common.component.TagChip
import com.griff.subscriptions.presentation.common.format.DateFormatter
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.obligations.DeadlineStatus
import com.griff.subscriptions.presentation.obligations.DeadlineUrgency
import com.griff.subscriptions.presentation.obligations.ObligationListItem
import com.griff.subscriptions.presentation.theme.GriffTheme
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews
import java.time.LocalDate

/**
 * A single obligation row: category icon, name, tag, the one date that matters and the amount.
 *
 * The tag replaces a spelled out category on the row - printing both "OC" and "Ubezpieczenie
 * pojazdu" would say the same thing twice - and an approaching deadline adds one quiet line below,
 * never a second badge.
 */
@Composable
internal fun ObligationListItemRow(
    item: ObligationListItem,
    modifier: Modifier = Modifier,
) {
    val amountText = MoneyFormatter.format(item.amount)
    val tag = Tags.of(item.category)
    val dateText = item.dateText()
    val description = stringResource(
        R.string.obligations_item_description,
        item.name,
        stringResource(tag.labelRes),
        amountText,
        dateText,
    )

    EntryRow(
        title = item.name,
        amount = amountText,
        modifier = modifier.semantics { contentDescription = description },
        leading = { ObligationIcon(category = item.category) },
        supporting = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    TagChip(style = tag)
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val deadline = item.deadline
                if (deadline != null && deadline.urgency != DeadlineUrgency.NORMAL) {
                    DeadlineNote(deadline)
                }
            }
        },
    )
}

/**
 * The deadline warning: an icon, the accent color and the words that say the same thing.
 *
 * Never the color on its own - a user who cannot tell amber from red still reads "Wygasa za 5 dni".
 */
@Composable
private fun DeadlineNote(deadline: DeadlineStatus) {
    val color = deadline.urgency.color()
    val text = when {
        deadline.daysPluralRes != null ->
            pluralStringResource(deadline.daysPluralRes, deadline.days, deadline.days)

        deadline.textRes != null -> stringResource(deadline.textRes)
        else -> return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Icon(
            imageVector = when (deadline.urgency) {
                DeadlineUrgency.OVERDUE -> Icons.Default.ErrorOutline
                else -> Icons.AutoMirrored.Filled.ScheduleSend
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(NoteIconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun DeadlineUrgency.color(): Color = when (this) {
    DeadlineUrgency.OVERDUE -> MaterialTheme.colorScheme.error
    DeadlineUrgency.SOON -> GriffTheme.colors.warning
    DeadlineUrgency.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * The one date the row shows.
 *
 * A settled record is about the payment that already happened; an open one about the deadline ahead.
 * The cover end is shown when it is the only thing the record has to say.
 */
@Composable
private fun ObligationListItem.dateText(): String = when {
    isPaid && paymentDate != null -> stringResource(
        R.string.obligations_paid_on,
        DateFormatter.formatShortDate(paymentDate),
    )

    !isPaid && dueDate != null -> stringResource(
        R.string.obligations_due_on,
        DateFormatter.formatShortDate(dueDate),
    )

    validUntil != null -> stringResource(
        R.string.obligations_valid_until,
        DateFormatter.formatShortDate(validUntil),
    )

    else -> stringResource(R.string.obligations_no_dates)
}

private val NoteIconSize = 14.dp

@ThemePreviews
@Composable
private fun ObligationListItemRowPreview() {
    GriffThemePreview {
        Column(modifier = Modifier.padding(vertical = Spacing.Small)) {
            ObligationListItemRow(
                item = ObligationListItem(
                    id = "1",
                    name = "OC Ford",
                    category = ObligationCategory.VEHICLE_INSURANCE,
                    amount = Money.ofUnits(1_240),
                    isPaid = true,
                    paymentDate = LocalDate.of(2026, 3, 12),
                    dueDate = null,
                    validUntil = LocalDate.of(2027, 3, 11),
                    deadline = DeadlineStatus(DeadlineUrgency.NORMAL),
                ),
            )
            ObligationListItemRow(
                item = ObligationListItem(
                    id = "2",
                    name = "Podatek od gruntu",
                    category = ObligationCategory.LAND_TAX,
                    amount = Money.ofUnits(320),
                    isPaid = false,
                    paymentDate = null,
                    dueDate = LocalDate.of(2026, 9, 15),
                    validUntil = null,
                    deadline = DeadlineStatus(
                        urgency = DeadlineUrgency.SOON,
                        daysPluralRes = R.plurals.deadline_due_in,
                        days = 5,
                    ),
                ),
            )
            ObligationListItemRow(
                item = ObligationListItem(
                    id = "3",
                    name = "Ubezpieczenie drona",
                    category = ObligationCategory.DRONE_INSURANCE,
                    amount = Money.ofUnits(190),
                    isPaid = true,
                    paymentDate = LocalDate.of(2025, 8, 2),
                    dueDate = null,
                    validUntil = LocalDate.of(2026, 8, 1),
                    deadline = DeadlineStatus(
                        urgency = DeadlineUrgency.OVERDUE,
                        textRes = R.string.deadline_expired,
                    ),
                ),
            )
        }
    }
}
