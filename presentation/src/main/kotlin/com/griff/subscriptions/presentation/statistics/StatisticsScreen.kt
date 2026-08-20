package com.griff.subscriptions.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.component.EmptyState
import com.griff.subscriptions.presentation.common.component.FullScreenLoading
import com.griff.subscriptions.presentation.common.component.GriffSnackbarHost
import com.griff.subscriptions.presentation.common.component.accentSegmentedButtonColors
import com.griff.subscriptions.presentation.common.component.showMessage
import com.griff.subscriptions.presentation.common.format.DateFormatter
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.common.resolve
import com.griff.subscriptions.presentation.statistics.components.CategoryBreakdown
import com.griff.subscriptions.presentation.statistics.components.ForecastChart
import com.griff.subscriptions.presentation.statistics.components.RankedSubscriptionRow
import com.griff.subscriptions.presentation.statistics.components.SummaryCards
import com.griff.subscriptions.presentation.statistics.components.UpcomingChargeRow
import com.griff.subscriptions.presentation.theme.GriffTheme
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.TallThemePreviews
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatisticsRoute(
    onOpenDrawer: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsScreen(
        state = state,
        onPeriodChange = viewModel::onPeriodChange,
        onOpenDrawer = onOpenDrawer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsScreen(
    state: StatisticsUiState,
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
                            contentDescription = stringResource(R.string.home_open_menu),
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

                state.isEmpty -> EmptyState(
                    icon = Icons.Default.InsertChartOutlined,
                    title = stringResource(R.string.statistics_empty_title),
                    description = stringResource(R.string.statistics_empty_description),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> StatisticsContent(
                    state = state,
                    onPeriodChange = onPeriodChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsContent(
    state: StatisticsUiState,
    onPeriodChange: (StatisticsPeriod) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraLarge),
    ) {
        SummaryCards(totals = state.totals)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val periods = StatisticsPeriod.entries
            periods.forEachIndexed { index, period ->
                SegmentedButton(
                    selected = period == state.period,
                    onClick = { onPeriodChange(period) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                    colors = accentSegmentedButtonColors(),
                ) {
                    Text(stringResource(Labels.statisticsPeriod(period)))
                }
            }
        }

        StatisticsSection(title = stringResource(R.string.statistics_forecast_title)) {
            val singleMonth = state.forecast.singleOrNull()
            if (state.hasForecast && singleMonth != null) {
                Text(
                    text = stringResource(
                        R.string.statistics_forecast_month_total,
                        DateFormatter.formatMonthAndYear(singleMonth.month),
                        MoneyFormatter.format(singleMonth.amount),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.statistics_forecast_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (state.hasForecast) {
                ForecastChart(bars = state.forecast)
            } else {
                Text(
                    text = stringResource(R.string.statistics_forecast_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.subscriptionsWithoutBillingDate > 0) {
                // Not a failure and not a warning: the forecast simply cannot cover these.
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
                        text = pluralStringResource(
                            R.plurals.statistics_without_date,
                            state.subscriptionsWithoutBillingDate,
                            state.subscriptionsWithoutBillingDate,
                            MoneyFormatter.format(state.unscheduledMonthlyCost),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.upcomingCharges.isNotEmpty()) {
            StatisticsSection(title = stringResource(R.string.statistics_upcoming_title)) {
                state.upcomingCharges.forEach { charge ->
                    UpcomingChargeRow(charge = charge)
                }
            }
        }

        StatisticsSection(
            title = stringResource(R.string.statistics_average_title),
            description = stringResource(R.string.statistics_average_description),
        ) {
            Text(
                text = stringResource(
                    R.string.amount_per_month,
                    MoneyFormatter.format(state.totals.monthly),
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (state.categories.isNotEmpty()) {
            StatisticsSection(title = stringResource(R.string.statistics_categories_title)) {
                CategoryBreakdown(categories = state.categories)
            }
        }

        if (state.topSubscriptions.isNotEmpty()) {
            StatisticsSection(
                title = stringResource(R.string.statistics_top_title),
                description = stringResource(R.string.statistics_top_description),
            ) {
                state.topSubscriptions.forEach { subscription ->
                    RankedSubscriptionRow(subscription = subscription)
                }
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
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
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

private val NoteIconSize = 16.dp

@TallThemePreviews
@Composable
private fun StatisticsScreenPreview() {
    GriffThemePreview {
        StatisticsScreen(
            state = StatisticsUiState(
                isLoading = false,
                totals = SubscriptionTotals(Money.ofUnits(286, 40), Money.ofUnits(3_436, 80), 12),
                forecast = List(12) { index ->
                    ForecastBar(
                        month = YearMonth.of(2026, 8).plusMonths(index.toLong()),
                        amount = Money.ofUnits(120L + index * 25),
                    )
                },
                upcomingCharges = listOf(
                    UpcomingCharge("1", "Spotify", "spotify", LocalDate.of(2026, 8, 25), Money.ofUnits(34, 99)),
                ),
                categories = listOf(
                    CategoryShare(ProviderCategory.VIDEO, Money.ofUnits(120), 0.42f, 0),
                    CategoryShare(ProviderCategory.MUSIC, Money.ofUnits(70), 0.24f, 1),
                    CategoryShare(ProviderCategory.HOSTING, Money.ofUnits(96, 40), 0.34f, 2),
                ),
                topSubscriptions = listOf(
                    RankedSubscription("1", "Google Workspace", "google_workspace", BillingPeriod.YEARLY, Money.ofUnits(86)),
                    RankedSubscription("2", "Netflix", "netflix", BillingPeriod.MONTHLY, Money.ofUnits(67)),
                ),
                unscheduledMonthlyCost = Money.ofUnits(49, 92),
                subscriptionsWithoutBillingDate = 2,
            ),
            onPeriodChange = {},
            onOpenDrawer = {},
        )
    }
}
