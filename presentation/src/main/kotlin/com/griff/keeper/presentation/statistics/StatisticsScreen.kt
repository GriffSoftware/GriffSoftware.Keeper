package com.griff.keeper.presentation.statistics

import com.griff.keeper.presentation.common.format.formatted

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.keeper.domain.model.ExpensePeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationTotals
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.domain.statistics.ExpenseSource
import com.griff.keeper.domain.statistics.StatisticsPeriod
import com.griff.keeper.domain.statistics.StatisticsScope
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Labels
import com.griff.keeper.presentation.common.component.EmptyState
import com.griff.keeper.presentation.common.component.FullScreenLoading
import com.griff.keeper.presentation.common.component.GriffCard
import com.griff.keeper.presentation.common.component.GriffHeroCard
import com.griff.keeper.presentation.common.component.GriffSegmentedControl
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.HeroStatTile
import com.griff.keeper.presentation.common.component.SegmentOption
import com.griff.keeper.presentation.common.component.TagStyle
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.common.format.PeriodFormatter
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.statistics.components.BreakdownAmount
import com.griff.keeper.presentation.statistics.components.ExpenseSeries
import com.griff.keeper.presentation.statistics.components.MonthlyExpenseChart
import com.griff.keeper.presentation.statistics.components.ObligationSummaryCards
import com.griff.keeper.presentation.statistics.components.RankedExpenseRow
import com.griff.keeper.presentation.statistics.components.RankedSubscriptionRow
import com.griff.keeper.presentation.statistics.components.SpendingBreakdown
import com.griff.keeper.presentation.statistics.components.SubscriptionSummaryCards
import com.griff.keeper.presentation.statistics.components.SummaryCard
import com.griff.keeper.presentation.statistics.components.UpcomingChargeRow
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.TagAccent
import com.griff.keeper.presentation.theme.TallThemePreviews
import java.time.YearMonth

@Composable
fun StatisticsRoute(
    onOpenDrawer: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsScreen(
        state = state,
        onScopeChange = viewModel::onScopeChange,
        onPeriodChange = viewModel::onPeriodChange,
        onOpenDrawer = onOpenDrawer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsScreen(
    state: StatisticsUiState,
    onScopeChange: (StatisticsScope) -> Unit,
    onPeriodChange: (StatisticsPeriod) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message?.resolve()

    LaunchedEffect(message) {
        if (message != null) snackbarHostState.showMessage(message)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                        )
                    }
                },
            )
        },
        snackbarHost = { GriffSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when {
                state.isLoading -> FullScreenLoading()

                else -> StatisticsContent(
                    state = state,
                    onScopeChange = onScopeChange,
                    onPeriodChange = onPeriodChange,
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    state: StatisticsUiState,
    onScopeChange: (StatisticsScope) -> Unit,
    onPeriodChange: (StatisticsPeriod) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraLarge),
    ) {
        // Scope first, then period: which part of the finances, then how wide a window.
        ScopeSelector(selected = state.scope, onSelect = onScopeChange)
        PeriodSelector(selected = state.period, onSelect = onPeriodChange)

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Default.InsertChartOutlined,
                title = stringResource(R.string.statistics_empty_title),
                description = stringResource(R.string.statistics_empty_description),
            )
            return@Column
        }

        state.combined?.let { CombinedSections(combined = it, window = state.window) }
        state.subscriptions?.let { SubscriptionSections(subscriptions = it, scope = state.scope) }
        state.obligations?.let { ObligationSections(obligations = it, scope = state.scope) }
    }
}

@Composable
private fun ScopeSelector(
    selected: StatisticsScope,
    onSelect: (StatisticsScope) -> Unit,
) {
    GriffSegmentedControl(
        options = StatisticsScope.entries.map {
            SegmentOption(value = it, label = stringResource(Labels.statisticsScope(it)))
        },
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PeriodSelector(
    selected: StatisticsPeriod,
    onSelect: (StatisticsPeriod) -> Unit,
) {
    GriffSegmentedControl(
        options = StatisticsPeriod.entries.map {
            SegmentOption(value = it, label = stringResource(Labels.statisticsPeriod(it)))
        },
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The combined view.
 *
 * Every figure here is labelled with what kind of number it is: the subscription total says it is an
 * estimate, the obligation total says it actually happened, and the sum of the two carries a note
 * that says the same thing in words. Without those labels the total would look like a bank statement.
 */
@Composable
private fun CombinedSections(
    combined: CombinedStatisticsUi,
    window: ExpensePeriod?,
) {
    GriffHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2)) {
            Text(
                text = stringResource(R.string.statistics_combined_title) +
                    (window?.let { " · ${PeriodFormatter.format(it)}" } ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = GriffGradients.OnAccent.copy(alpha = 0.82f),
            )
            Text(
                text = combined.total.formatted(),
                style = MaterialTheme.typography.displaySmall,
                color = GriffGradients.OnAccent,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                HeroStatTile(
                    label = stringResource(R.string.statistics_combined_subscriptions_label),
                    value = combined.estimatedSubscriptions.formatted(),
                    note = stringResource(R.string.statistics_combined_subscriptions_note),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                HeroStatTile(
                    label = stringResource(R.string.statistics_combined_obligations_label),
                    value = combined.paidObligations.formatted(),
                    note = stringResource(R.string.statistics_combined_obligations_note),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }

            Row(
                modifier = Modifier.padding(top = Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = GriffGradients.OnAccent.copy(alpha = 0.85f),
                    modifier = Modifier.size(NoteIconSize),
                )
                Text(
                    text = stringResource(R.string.statistics_combined_total_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = GriffGradients.OnAccent.copy(alpha = 0.85f),
                )
            }
        }
    }

    if (combined.hasChartData) {
        StatisticsSection(
            title = stringResource(R.string.statistics_combined_chart_title),
            description = stringResource(R.string.statistics_combined_chart_description),
        ) {
            MonthlyExpenseChart(
                bars = combined.months,
                series = listOf(ExpenseSeries.SUBSCRIPTIONS, ExpenseSeries.OBLIGATIONS),
            )
        }
    }

    if (combined.topExpenses.isNotEmpty()) {
        StatisticsSection(title = stringResource(R.string.statistics_top_title)) {
            val maxAmount = combined.topExpenses.maxOf { it.amount.minorUnits }
            combined.topExpenses.forEach { expense ->
                RankedExpenseRow(expense = expense, shareOfMax = expense.amount.shareOf(Money.ofMinorUnits(maxAmount)))
            }
        }
    }
}

@Composable
private fun SubscriptionSections(
    subscriptions: SubscriptionStatisticsUi,
    scope: StatisticsScope,
) {
    if (subscriptions.totals.subscriptionCount == 0) return

    SubscriptionSummaryCards(totals = subscriptions.totals)

    // The forecast is a subscription-only view of real charge dates; in the combined scope the
    // monthly chart above already carries the time dimension.
    if (scope == StatisticsScope.SUBSCRIPTIONS) {
        StatisticsSection(title = stringResource(R.string.statistics_forecast_title)) {
            val singleMonth = subscriptions.forecast.singleOrNull()
            if (subscriptions.hasForecast && singleMonth != null) {
                Text(
                    text = stringResource(
                        R.string.statistics_forecast_month_total,
                        DateFormatter.formatMonthAndYear(singleMonth.month),
                        singleMonth.amount.formatted(),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.statistics_forecast_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (subscriptions.hasForecast) {
                MonthlyExpenseChart(
                    bars = subscriptions.forecast.map {
                        ExpenseBar(
                            month = it.month,
                            subscriptions = it.amount,
                            obligations = Money.ZERO,
                        )
                    },
                    series = listOf(ExpenseSeries.SUBSCRIPTIONS),
                )
                Text(
                    text = stringResource(R.string.statistics_forecast_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.Small),
                )
            } else {
                Text(
                    text = stringResource(R.string.statistics_forecast_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (subscriptions.subscriptionsWithoutBillingDate > 0) {
                Note(
                    text = pluralStringResource(
                        R.plurals.statistics_without_date,
                        subscriptions.subscriptionsWithoutBillingDate,
                        subscriptions.subscriptionsWithoutBillingDate,
                        subscriptions.unscheduledMonthlyCost.formatted(),
                    ),
                )
            }
        }

        if (subscriptions.upcomingCharges.isNotEmpty()) {
            StatisticsSection(title = stringResource(R.string.statistics_upcoming_title)) {
                subscriptions.upcomingCharges.forEach { charge -> UpcomingChargeRow(charge = charge) }
            }
        }

        StatisticsSection(
            title = stringResource(R.string.statistics_average_title),
            description = stringResource(R.string.statistics_average_description),
        ) {
            Text(
                text = stringResource(
                    R.string.amount_per_month,
                    subscriptions.totals.monthly.formatted(),
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }

    if (subscriptions.categories.isNotEmpty()) {
        StatisticsSection(title = stringResource(R.string.statistics_categories_title)) {
            SpendingBreakdown(
                entries = subscriptions.categories,
                amountStyle = BreakdownAmount.PER_MONTH,
            )
        }
    }

    // In the combined scope the ranking above already mixes both sources.
    if (scope == StatisticsScope.SUBSCRIPTIONS && subscriptions.topSubscriptions.isNotEmpty()) {
        StatisticsSection(
            title = stringResource(R.string.statistics_top_title),
            description = stringResource(R.string.statistics_top_description),
        ) {
            val maxAmount = subscriptions.topSubscriptions.maxOf { it.monthlyEquivalent.minorUnits }
            subscriptions.topSubscriptions.forEach { subscription ->
                RankedSubscriptionRow(
                    subscription = subscription,
                    shareOfMax = subscription.monthlyEquivalent.shareOf(Money.ofMinorUnits(maxAmount)),
                )
            }
        }
    }
}

@Composable
private fun ObligationSections(
    obligations: ObligationStatisticsUi,
    scope: StatisticsScope,
) {
    if (obligations.totals.count == 0) return

    if (scope == StatisticsScope.OBLIGATIONS) {
        ObligationSummaryCards(totals = obligations.totals)

        StatisticsSection(
            title = stringResource(R.string.statistics_obligations_payments_title),
            description = stringResource(R.string.statistics_obligations_payments_description),
        ) {
            if (obligations.hasPayments) {
                MonthlyExpenseChart(
                    bars = obligations.payments,
                    series = listOf(ExpenseSeries.OBLIGATIONS),
                )
            } else {
                Text(
                    text = stringResource(R.string.statistics_obligations_payments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (obligations.tags.isNotEmpty()) {
        StatisticsSection(title = stringResource(R.string.statistics_obligations_categories_title)) {
            SpendingBreakdown(
                entries = obligations.tags,
                amountStyle = BreakdownAmount.ABSOLUTE,
            )
        }
    }

    if (scope == StatisticsScope.OBLIGATIONS && obligations.topObligations.isNotEmpty()) {
        StatisticsSection(
            title = stringResource(R.string.statistics_obligations_top_title),
            description = stringResource(R.string.statistics_obligations_top_description),
        ) {
            val maxAmount = obligations.topObligations.maxOf { it.amount.minorUnits }
            obligations.topObligations.forEach { expense ->
                RankedExpenseRow(expense = expense, shareOfMax = expense.amount.shareOf(Money.ofMinorUnits(maxAmount)))
            }
        }
    }
}

@Composable
private fun StatisticsSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    GriffCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.Large),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

/** An aside, not a warning: an informative note about how a number was arrived at. */
@Composable
private fun Note(text: String) {
    Row(
        modifier = Modifier.padding(top = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = GriffTheme.colors.info,
            modifier = Modifier.size(NoteIconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val NoteIconSize = 16.dp

@TallThemePreviews
@Composable
private fun StatisticsScreenAllPreview() {
    GriffThemePreview {
        StatisticsScreen(
            state = StatisticsUiState(
                isLoading = false,
                scope = StatisticsScope.ALL,
                period = StatisticsPeriod.YEAR,
                window = ExpensePeriod.Year(2026),
                subscriptions = SubscriptionStatisticsUi(
                    totals = SubscriptionTotals(
                        Money.ofUnits(286, 40),
                        Money.ofUnits(3_436, 80),
                        12,
                    ),
                    categories = listOf(
                        SpendingShare(
                            TagStyle(R.string.category_video, TagAccent.RED),
                            Money.ofUnits(120),
                            0.42f,
                        ),
                        SpendingShare(
                            TagStyle(R.string.category_music, TagAccent.EMERALD),
                            Money.ofUnits(70),
                            0.24f,
                        ),
                    ),
                ),
                obligations = ObligationStatisticsUi(
                    totals = ObligationTotals(
                        paid = Money.ofUnits(4_820),
                        outstanding = Money.ofUnits(320),
                        paidCount = 4,
                        outstandingCount = 1,
                        largestPaid = Money.ofUnits(1_420),
                    ),
                    tags = listOf(
                        SpendingShare(
                            TagStyle(R.string.tag_vehicle_insurance, TagAccent.BLUE),
                            Money.ofUnits(2_400),
                            0.5f,
                        ),
                        SpendingShare(
                            TagStyle(R.string.tag_tax, TagAccent.AMBER),
                            Money.ofUnits(780),
                            0.16f,
                        ),
                    ),
                ),
                combined = CombinedStatisticsUi(
                    estimatedSubscriptions = Money.ofUnits(3_436, 80),
                    paidObligations = Money.ofUnits(4_820),
                    total = Money.ofUnits(8_256, 80),
                    months = List(12) { index ->
                        ExpenseBar(
                            month = YearMonth.of(2026, 1).plusMonths(index.toLong()),
                            subscriptions = Money.ofUnits(286, 40),
                            obligations = if (index % 4 == 2) {
                                Money.ofUnits(1_240L - index * 40)
                            } else {
                                Money.ZERO
                            },
                        )
                    },
                    topExpenses = listOf(
                        RankedExpenseItem(
                            "1",
                            "OC Ford",
                            Money.ofUnits(1_240),
                            ExpenseSource.OBLIGATION,
                            R.string.statistics_source_obligation,
                        ),
                        RankedExpenseItem(
                            "2",
                            "Google Workspace",
                            Money.ofUnits(86),
                            ExpenseSource.SUBSCRIPTION,
                            R.string.statistics_source_subscription,
                        ),
                    ),
                ),
            ),
            onScopeChange = {},
            onPeriodChange = {},
            onOpenDrawer = {},
        )
    }
}

@TallThemePreviews
@Composable
private fun StatisticsScreenObligationsPreview() {
    GriffThemePreview {
        StatisticsScreen(
            state = StatisticsUiState(
                isLoading = false,
                scope = StatisticsScope.OBLIGATIONS,
                period = StatisticsPeriod.YEAR,
                window = ExpensePeriod.Year(2026),
                obligations = ObligationStatisticsUi(
                    totals = ObligationTotals(
                        paid = Money.ofUnits(4_820),
                        outstanding = Money.ofUnits(320),
                        paidCount = 4,
                        outstandingCount = 1,
                        largestPaid = Money.ofUnits(1_420),
                    ),
                    payments = List(12) { index ->
                        ExpenseBar(
                            month = YearMonth.of(2026, 1).plusMonths(index.toLong()),
                            subscriptions = Money.ZERO,
                            obligations = if (index % 3 == 2) {
                                Money.ofUnits(1_240L - index * 60)
                            } else {
                                Money.ZERO
                            },
                        )
                    },
                    tags = listOf(
                        SpendingShare(
                            TagStyle(R.string.tag_vehicle_insurance, TagAccent.BLUE),
                            Money.ofUnits(2_400),
                            0.5f,
                        ),
                        SpendingShare(
                            TagStyle(R.string.tag_home_insurance, TagAccent.EMERALD),
                            Money.ofUnits(640),
                            0.13f,
                        ),
                    ),
                    topObligations = listOf(
                        RankedExpenseItem(
                            "1",
                            "OC Ford",
                            Money.ofUnits(1_240),
                            ExpenseSource.OBLIGATION,
                            R.string.statistics_source_obligation,
                        ),
                    ),
                ),
            ),
            onScopeChange = {},
            onPeriodChange = {},
            onOpenDrawer = {},
        )
    }
}
