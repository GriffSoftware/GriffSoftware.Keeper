package com.griff.subscriptions.presentation.obligations.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.component.accentSegmentedButtonColors
import com.griff.subscriptions.presentation.common.format.PeriodFormatter
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews
import java.time.LocalDate
import java.time.YearMonth

/** Whether the period selector steps through months or through years. */
enum class PeriodMode { MONTH, YEAR }

/**
 * Period picker: a month/year switch above a stepper for the selected window.
 *
 * Two controls rather than one, because the two questions are different - "how wide a window" and
 * "which one" - and a single dropdown of every month of every year would be unusable. Switching the
 * mode keeps the user where they are in time: the month view opens on a month of the year they were
 * looking at.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodSelector(
    period: ExpensePeriod,
    today: LocalDate,
    onPeriodChange: (ExpensePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PeriodMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = period.mode() == mode,
                    onClick = { onPeriodChange(period.switchedTo(mode, today)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PeriodMode.entries.size,
                    ),
                    colors = accentSegmentedButtonColors(),
                ) {
                    Text(
                        stringResource(
                            when (mode) {
                                PeriodMode.MONTH -> R.string.period_mode_month
                                PeriodMode.YEAR -> R.string.period_mode_year
                            },
                        ),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onPeriodChange(period.shifted(-1)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.period_previous),
                )
            }
            Text(
                text = PeriodFormatter.format(period),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onPeriodChange(period.shifted(1)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.period_next),
                )
            }
        }
    }
}

private fun ExpensePeriod.mode(): PeriodMode = when (this) {
    is ExpensePeriod.Month -> PeriodMode.MONTH
    is ExpensePeriod.Year, is ExpensePeriod.Range -> PeriodMode.YEAR
}

/** Keeps the user in the same part of the calendar when the width of the window changes. */
private fun ExpensePeriod.switchedTo(mode: PeriodMode, today: LocalDate): ExpensePeriod =
    when (mode) {
        PeriodMode.MONTH -> asMonth(today)
        PeriodMode.YEAR -> asYear(today)
    }

@ThemePreviews
@Composable
private fun PeriodSelectorPreview() {
    GriffThemePreview {
        Column(modifier = Modifier.padding(Spacing.Large)) {
            val today = LocalDate.of(2026, 8, 21)
            PeriodSelector(period = ExpensePeriod.Year(2026), today = today, onPeriodChange = {})
            PeriodSelector(
                period = ExpensePeriod.Month(YearMonth.of(2026, 8)),
                today = today,
                onPeriodChange = {},
                modifier = Modifier.padding(top = Spacing.Large),
            )
        }
    }
}
