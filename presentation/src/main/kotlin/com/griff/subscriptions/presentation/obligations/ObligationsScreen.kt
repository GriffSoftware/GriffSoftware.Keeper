package com.griff.subscriptions.presentation.obligations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.ObligationTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Tags
import com.griff.subscriptions.presentation.common.UiMessage
import com.griff.subscriptions.presentation.common.component.EmptyState
import com.griff.subscriptions.presentation.common.component.FullScreenLoading
import com.griff.subscriptions.presentation.common.component.GriffSnackbarHost
import com.griff.subscriptions.presentation.common.component.SearchField
import com.griff.subscriptions.presentation.common.component.TagFilterOption
import com.griff.subscriptions.presentation.common.component.TagFilterRow
import com.griff.subscriptions.presentation.common.component.showMessage
import com.griff.subscriptions.presentation.common.format.PeriodFormatter
import com.griff.subscriptions.presentation.common.resolve
import com.griff.subscriptions.presentation.obligations.components.ObligationListItemRow
import com.griff.subscriptions.presentation.obligations.components.ObligationTotalsBar
import com.griff.subscriptions.presentation.obligations.components.PeriodSelector
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews
import java.time.LocalDate

/** Entry point wired to the [ObligationsViewModel]; keeps the screen free of DI concerns. */
@Composable
fun ObligationsRoute(
    onOpenDrawer: () -> Unit,
    onObligationClick: (String) -> Unit,
    onAddObligation: () -> Unit,
    pendingMessage: UiMessage? = null,
    onPendingMessageShown: () -> Unit = {},
    viewModel: ObligationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObligationsScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onTagChange = viewModel::onTagChange,
        onPeriodChange = viewModel::onPeriodChange,
        onOpenDrawer = onOpenDrawer,
        onObligationClick = onObligationClick,
        onAddObligation = onAddObligation,
        pendingMessage = pendingMessage,
        onPendingMessageShown = onPendingMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObligationsScreen(
    state: ObligationsUiState,
    onQueryChange: (String) -> Unit,
    onTagChange: (ObligationTag?) -> Unit,
    onPeriodChange: (ExpensePeriod) -> Unit,
    onOpenDrawer: () -> Unit,
    onObligationClick: (String) -> Unit,
    onAddObligation: () -> Unit,
    pendingMessage: UiMessage? = null,
    onPendingMessageShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message?.resolve()
    val externalMessage = pendingMessage?.resolve()

    LaunchedEffect(message) {
        if (message != null) snackbarHostState.showMessage(message)
    }

    LaunchedEffect(externalMessage) {
        if (externalMessage != null) {
            snackbarHostState.showMessage(externalMessage)
            onPendingMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.obligations_title)) },
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
        bottomBar = {
            if (!state.isEmpty) {
                ObligationTotalsBar(
                    period = state.period,
                    totals = state.totals,
                    isNarrowed = state.isNarrowed,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddObligation,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.obligations_add),
                )
            }
        },
        snackbarHost = { GriffSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        ObligationsContent(
            state = state,
            onQueryChange = onQueryChange,
            onTagChange = onTagChange,
            onPeriodChange = onPeriodChange,
            onObligationClick = onObligationClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}

@Composable
private fun ObligationsContent(
    state: ObligationsUiState,
    onQueryChange: (String) -> Unit,
    onTagChange: (ObligationTag?) -> Unit,
    onPeriodChange: (ExpensePeriod) -> Unit,
    onObligationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SearchField(
            query = state.query,
            placeholder = stringResource(R.string.obligations_search_placeholder),
            onQueryChange = onQueryChange,
        )

        // Filters are only useful once something is stored; an empty app keeps its first screen calm.
        if (!state.isEmpty) {
            PeriodSelector(
                period = state.period,
                today = state.today,
                onPeriodChange = onPeriodChange,
                modifier = Modifier.padding(
                    start = Spacing.Large,
                    end = Spacing.Large,
                    bottom = Spacing.Small,
                ),
            )

            if (state.availableTags.size > 1) {
                TagFilterRow(
                    options = state.availableTags.map {
                        TagFilterOption(value = it, style = Tags.of(it))
                    },
                    selected = state.selectedTag,
                    onSelect = onTagChange,
                    modifier = Modifier.padding(bottom = Spacing.Small),
                )
            }
        }

        when {
            state.isLoading -> FullScreenLoading()

            state.isEmpty -> EmptyState(
                icon = Icons.Default.VerifiedUser,
                title = stringResource(R.string.obligations_empty_title),
                description = stringResource(R.string.obligations_empty_description),
            )

            state.items.isEmpty() -> EmptyState(
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.no_results_title),
                description = noResultsDescription(state),
            )

            else -> ObligationList(
                state = state,
                onObligationClick = onObligationClick,
            )
        }
    }
}

/**
 * Why the list is empty, naming the filters that are actually active.
 *
 * The period is always part of the message, because a record can be perfectly fine and simply belong
 * to a different year - which is a very different thing from having no records at all.
 */
@Composable
private fun noResultsDescription(state: ObligationsUiState): String {
    val period = PeriodFormatter.format(state.period)
    val tag = state.selectedTag?.let { stringResource(Tags.of(it).labelRes) }
    return when {
        tag != null && state.query.isNotBlank() -> stringResource(
            R.string.obligations_no_results_tag_and_query,
            tag,
            state.query,
            period,
        )

        tag != null -> stringResource(R.string.obligations_no_results_tag, tag, period)

        state.query.isNotBlank() ->
            stringResource(R.string.obligations_no_results_query, state.query, period)

        else -> stringResource(R.string.obligations_no_results_period, period)
    }
}

@Composable
private fun ObligationList(
    state: ObligationsUiState,
    onObligationClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room for the floating action button, which hovers above the summary bar.
        contentPadding = PaddingValues(bottom = FabClearance),
    ) {
        items(items = state.items, key = { it.id }) { item ->
            ObligationListItemRow(
                item = item,
                modifier = Modifier.clickable { onObligationClick(item.id) },
            )
        }
    }
}

private val FabClearance = 88.dp

@ThemePreviews
@Composable
private fun ObligationsScreenPreview() {
    GriffThemePreview {
        ObligationsScreen(
            state = ObligationsUiState(
                period = ExpensePeriod.Year(2026),
                today = LocalDate.of(2026, 8, 21),
                isLoading = false,
                availableTags = listOf(
                    ObligationTag.VEHICLE,
                    ObligationTag.HOME,
                    ObligationTag.TAX,
                    ObligationTag.DRONE,
                ),
                items = listOf(
                    ObligationListItem(
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
                    ObligationListItem(
                        id = "2",
                        name = "Ubezpieczenie domu",
                        category = ObligationCategory.HOME_INSURANCE,
                        amount = Money.ofUnits(780),
                        isPaid = true,
                        paymentDate = LocalDate.of(2026, 6, 3),
                        dueDate = null,
                        validUntil = LocalDate.of(2027, 6, 2),
                        deadline = DeadlineStatus(DeadlineUrgency.NORMAL),
                    ),
                    ObligationListItem(
                        id = "3",
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
                ),
                totals = ObligationTotals(
                    paid = Money.ofUnits(4_820),
                    outstanding = Money.ofUnits(320),
                    paidCount = 4,
                    outstandingCount = 1,
                    largestPaid = Money.ofUnits(1_420),
                ),
                totalCount = 5,
            ),
            onQueryChange = {},
            onTagChange = {},
            onPeriodChange = {},
            onOpenDrawer = {},
            onObligationClick = {},
            onAddObligation = {},
        )
    }
}

@ThemePreviews
@Composable
private fun ObligationsScreenEmptyPreview() {
    GriffThemePreview {
        ObligationsScreen(
            state = ObligationsUiState(
                period = ExpensePeriod.Year(2026),
                today = LocalDate.of(2026, 8, 21),
                isLoading = false,
            ),
            onQueryChange = {},
            onTagChange = {},
            onPeriodChange = {},
            onOpenDrawer = {},
            onObligationClick = {},
            onAddObligation = {},
        )
    }
}
