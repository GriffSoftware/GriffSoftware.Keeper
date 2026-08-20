package com.griff.subscriptions.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.UiMessage
import com.griff.subscriptions.presentation.common.component.EmptyState
import com.griff.subscriptions.presentation.common.component.FullScreenLoading
import com.griff.subscriptions.presentation.common.resolve
import com.griff.subscriptions.presentation.home.components.SubscriptionListItemRow
import com.griff.subscriptions.presentation.home.components.SubscriptionSearchField
import com.griff.subscriptions.presentation.home.components.SubscriptionTotalsBar
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import com.griff.subscriptions.presentation.theme.Spacing

/** Entry point wired to the [HomeViewModel]; keeps the screen itself free of DI concerns. */
@Composable
fun HomeRoute(
    onOpenDrawer: () -> Unit,
    onSubscriptionClick: (String) -> Unit,
    onAddSubscription: () -> Unit,
    pendingMessage: UiMessage? = null,
    onPendingMessageShown: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onOpenDrawer = onOpenDrawer,
        onSubscriptionClick = onSubscriptionClick,
        onAddSubscription = onAddSubscription,
        pendingMessage = pendingMessage,
        onPendingMessageShown = onPendingMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
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
        if (message != null) snackbarHostState.showSnackbar(message)
    }

    LaunchedEffect(externalMessage) {
        if (externalMessage != null) {
            snackbarHostState.showSnackbar(externalMessage)
            onPendingMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
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
        bottomBar = {
            if (state.items.isNotEmpty()) {
                SubscriptionTotalsBar(
                    totals = state.totals,
                    isFiltered = state.isFiltered,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSubscription) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_add_subscription),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        HomeContent(
            state = state,
            onQueryChange = onQueryChange,
            onSubscriptionClick = onSubscriptionClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onSubscriptionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SubscriptionSearchField(
            query = state.query,
            onQueryChange = onQueryChange,
        )

        when {
            state.isLoading -> FullScreenLoading()

            state.isEmpty -> EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(R.string.home_empty_title),
                description = stringResource(R.string.home_empty_description),
            )

            state.hasNoResults -> EmptyState(
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.home_no_results_title),
                description = stringResource(R.string.home_no_results_description, state.query),
            )

            else -> SubscriptionList(
                state = state,
                onSubscriptionClick = onSubscriptionClick,
            )
        }
    }
}

@Composable
private fun SubscriptionList(
    state: HomeUiState,
    onSubscriptionClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room for the floating action button, which hovers above the totals bar.
        contentPadding = PaddingValues(bottom = FabClearance),
    ) {
        items(items = state.items, key = { it.id }) { item ->
            SubscriptionListItemRow(
                item = item,
                modifier = Modifier.clickable { onSubscriptionClick(item.id) },
            )
        }
    }
}

private val FabClearance = 88.dp

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    GriffSubscriptionsTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(
                isLoading = false,
                items = listOf(
                    SubscriptionListItem("1", "Spotify", "spotify", BillingPeriod.MONTHLY, Money.ofUnits(34, 99)),
                    SubscriptionListItem("2", "Netflix", "netflix", BillingPeriod.MONTHLY, Money.ofUnits(67)),
                    SubscriptionListItem("3", "JetBrains", "jetbrains", BillingPeriod.YEARLY, Money.ofUnits(1_299)),
                ),
                totals = SubscriptionTotals(Money.ofUnits(210, 24), Money.ofUnits(2_522, 88), 3),
                totalSubscriptionCount = 3,
            ),
            onQueryChange = {},
            onOpenDrawer = {},
            onSubscriptionClick = {},
            onAddSubscription = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    GriffSubscriptionsTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(isLoading = false),
            onQueryChange = {},
            onOpenDrawer = {},
            onSubscriptionClick = {},
            onAddSubscription = {},
        )
    }
}

