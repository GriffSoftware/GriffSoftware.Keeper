package com.griff.keeper.presentation.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Tags
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.component.EmptyState
import com.griff.keeper.presentation.common.component.FullScreenLoading
import com.griff.keeper.presentation.common.component.GriffCard
import com.griff.keeper.presentation.common.component.GriffFab
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.SearchField
import com.griff.keeper.presentation.common.component.TagFilterOption
import com.griff.keeper.presentation.common.component.TagFilterRow
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.subscription.components.SubscriptionListItemRow
import com.griff.keeper.presentation.subscription.components.SubscriptionTotalsBar
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/** Entry point wired to the [SubscriptionViewModel]; keeps the screen itself free of DI concerns. */
@Composable
fun SubscriptionRoute(
    onOpenDrawer: () -> Unit,
    onSubscriptionClick: (String) -> Unit,
    onAddSubscription: () -> Unit,
    pendingMessage: UiMessage? = null,
    onPendingMessageShown: () -> Unit = {},
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SubscriptionScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCategoryChange = viewModel::onCategoryChange,
        onOpenDrawer = onOpenDrawer,
        onSubscriptionClick = onSubscriptionClick,
        onAddSubscription = onAddSubscription,
        pendingMessage = pendingMessage,
        onPendingMessageShown = onPendingMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionScreen(
    state: SubscriptionUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (ProviderCategory?) -> Unit,
    onOpenDrawer: () -> Unit,
    onSubscriptionClick: (String) -> Unit,
    onAddSubscription: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.subscriptions_title)) },
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
            floatingActionButton = {
                GriffFab(
                    onClick = onAddSubscription,
                    contentDescription = stringResource(R.string.subscriptions_add),
                )
            },
        ) { contentPadding ->
            SubscriptionContent(
                state = state,
                onQueryChange = onQueryChange,
                onCategoryChange = onCategoryChange,
                onSubscriptionClick = onSubscriptionClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        // Anchored to the screen's own bottom edge rather than through Scaffold's snackbarHost
        // slot, which leaves a gap above the FAB - the message is allowed to sit over the FAB
        // for its brief few seconds on screen instead.
        GriffSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
internal fun SubscriptionContent(
    state: SubscriptionUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (ProviderCategory?) -> Unit,
    onSubscriptionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (state.items.isNotEmpty()) {
            SubscriptionTotalsBar(
                totals = state.totals,
                isFiltered = state.isFiltered,
                totalSubscriptionCount = state.totalSubscriptionCount,
                items = state.items,
                modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small),
            )
        }

        SearchField(
            query = state.query,
            placeholder = stringResource(R.string.subscriptions_search_placeholder),
            onQueryChange = onQueryChange,
        )

        // Offered only once there is something to narrow down, so an empty app stays quiet.
        if (state.availableCategories.size > 1) {
            TagFilterRow(
                options = state.availableCategories.map {
                    TagFilterOption(value = it, style = Tags.of(it))
                },
                selected = state.selectedCategory,
                onSelect = onCategoryChange,
                modifier = Modifier.padding(top = Spacing.Small, bottom = Spacing.Small),
            )
        }

        when {
            state.isLoading -> FullScreenLoading()

            state.isEmpty -> EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(R.string.subscriptions_empty_title),
                description = stringResource(R.string.subscriptions_empty_description),
            )

            state.hasNoResults -> EmptyState(
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.no_results_title),
                description = noResultsDescription(state),
            )

            else -> SubscriptionList(
                state = state,
                onSubscriptionClick = onSubscriptionClick,
            )
        }
    }
}

/**
 * Why the list is empty, in the user's own terms.
 *
 * The message names the filters that are actually active, so a tag with no matches does not read as
 * a failed text search - and neither is mistaken for "you have no subscriptions yet".
 */
@Composable
private fun noResultsDescription(state: SubscriptionUiState): String {
    val category = state.selectedCategory?.let { stringResource(Tags.of(it).labelRes) }
    return when {
        category != null && state.query.isNotBlank() ->
            stringResource(R.string.subscriptions_no_results_category_and_query, category, state.query)

        category != null -> stringResource(R.string.subscriptions_no_results_category, category)

        else -> stringResource(R.string.subscriptions_no_results_query, state.query)
    }
}

@Composable
private fun SubscriptionList(
    state: SubscriptionUiState,
    onSubscriptionClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.Large,
            top = Spacing.Small,
            end = Spacing.Large,
            bottom = FabClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        items(items = state.items, key = { it.id }) { item ->
            GriffCard(
                modifier = Modifier.clickable { onSubscriptionClick(item.id) },
                contentPadding = PaddingValues(0.dp),
            ) {
                SubscriptionListItemRow(item = item)
            }
        }
    }
}

/** Room for the floating action button, which hovers above the list. */
private val FabClearance = 76.dp

@ThemePreviews
@Composable
private fun SubscriptionScreenPreview() {
    GriffThemePreview {
        SubscriptionScreen(
            state = SubscriptionUiState(
                isLoading = false,
                availableCategories = listOf(
                    ProviderCategory.VIDEO,
                    ProviderCategory.MUSIC,
                    ProviderCategory.SOFTWARE,
                ),
                items = listOf(
                    SubscriptionListItem(
                        "1", "Spotify", "spotify", ProviderCategory.MUSIC,
                        BillingPeriod.MONTHLY, Money.ofUnits(34, 99),
                    ),
                    SubscriptionListItem(
                        "2", "Netflix", "netflix", ProviderCategory.VIDEO,
                        BillingPeriod.MONTHLY, Money.ofUnits(67),
                    ),
                    SubscriptionListItem(
                        "3", "JetBrains", "jetbrains", ProviderCategory.SOFTWARE,
                        BillingPeriod.YEARLY, Money.ofUnits(1_299),
                    ),
                ),
                totals = SubscriptionTotals(Money.ofUnits(210, 24), Money.ofUnits(2_522, 88), 3),
                totalSubscriptionCount = 3,
            ),
            onQueryChange = {},
            onCategoryChange = {},
            onOpenDrawer = {},
            onSubscriptionClick = {},
            onAddSubscription = {},
        )
    }
}

@ThemePreviews
@Composable
private fun SubscriptionScreenEmptyPreview() {
    GriffThemePreview {
        SubscriptionScreen(
            state = SubscriptionUiState(isLoading = false),
            onQueryChange = {},
            onCategoryChange = {},
            onOpenDrawer = {},
            onSubscriptionClick = {},
            onAddSubscription = {},
        )
    }
}
