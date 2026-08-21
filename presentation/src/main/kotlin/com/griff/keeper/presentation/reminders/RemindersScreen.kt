package com.griff.keeper.presentation.reminders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.keeper.domain.reminder.ReminderSourceType
import com.griff.keeper.presentation.BuildConfig
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.EmptyState
import com.griff.keeper.presentation.common.component.FullScreenLoading
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.reminders.components.DebugToolsSection
import com.griff.keeper.presentation.reminders.components.GlobalRemindersCard
import com.griff.keeper.presentation.reminders.components.ReminderDefaultsSection
import com.griff.keeper.presentation.reminders.components.ReminderRow
import com.griff.keeper.presentation.reminders.components.RemindersDisabledCard
import com.griff.keeper.presentation.reminders.components.SystemNotificationsBlockedCard
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

@Composable
fun RemindersRoute(
    onOpenDrawer: () -> Unit,
    onSubscriptionClick: (String) -> Unit,
    onObligationClick: (String) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onPermissionResult(granted) }

    // The system dialog is never the first thing the user sees: it is preceded by a sentence saying
    // what the app wants it for, and it is only ever triggered by something the user just did.
    var isRationaleVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RemindersEvent.RequestNotificationPermission -> isRationaleVisible = true
            }
        }
    }

    // Notification settings can be changed while the app sits in the background, so the screen
    // re-reads them rather than trusting what it saw when it was opened.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenResumed() }

    val openNotificationSettings = rememberNotificationSettingsOpener()

    if (isRationaleVisible) {
        NotificationRationaleDialog(
            onConfirm = {
                isRationaleVisible = false
                val permission = postNotificationsPermission
                if (permission != null && !state.permissionDenied) {
                    permissionLauncher.launch(permission)
                } else {
                    openNotificationSettings()
                }
            },
            onDismiss = { isRationaleVisible = false },
        )
    }

    RemindersScreen(
        state = state,
        message = message,
        onOpenDrawer = onOpenDrawer,
        onGlobalEnabledChange = viewModel::onGlobalEnabledChange,
        onFilterChange = viewModel::onFilterChange,
        onRequestPermission = viewModel::onPermissionRequested,
        onOpenNotificationSettings = openNotificationSettings,
        onSendTestNotification = viewModel::onSendTestNotification,
        onItemClick = { row ->
            when (row.sourceType) {
                ReminderSourceType.SUBSCRIPTION -> onSubscriptionClick(row.id)
                ReminderSourceType.OBLIGATION -> onObligationClick(row.id)
            }
        },
        onMessageShown = viewModel::onMessageShown,
    )
}

/**
 * The reminders dashboard.
 *
 * Not a settings screen with a list bolted on: the order of the sections answers the questions a
 * user arrives with, in the order they ask them. Are reminders on? Is Android letting them through?
 * What is coming? And only then: what are the rules?
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemindersScreen(
    state: RemindersUiState,
    message: com.griff.keeper.presentation.common.UiMessage?,
    onOpenDrawer: () -> Unit,
    onGlobalEnabledChange: (Boolean) -> Unit,
    onFilterChange: (ReminderFilter) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSendTestNotification: () -> Unit,
    onItemClick: (ReminderRowUi) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resolved = message?.resolve()

    LaunchedEffect(resolved) {
        if (resolved != null) {
            snackbarHostState.showMessage(resolved)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_title)) },
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
            if (state.isLoading) {
                FullScreenLoading()
            } else {
                RemindersContent(
                    state = state,
                    onGlobalEnabledChange = onGlobalEnabledChange,
                    onFilterChange = onFilterChange,
                    onRequestPermission = onRequestPermission,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onSendTestNotification = onSendTestNotification,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun RemindersContent(
    state: RemindersUiState,
    onGlobalEnabledChange: (Boolean) -> Unit,
    onFilterChange: (ReminderFilter) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSendTestNotification: () -> Unit,
    onItemClick: (ReminderRowUi) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        item(key = "global") {
            GlobalRemindersCard(
                enabled = state.globalEnabled,
                onEnabledChange = onGlobalEnabledChange,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }

        // Two different failures, told apart on purpose: the app being switched off is the user's
        // own decision, Android blocking it is not.
        if (state.isBlockedBySystem) {
            item(key = "permission") {
                SystemNotificationsBlockedCard(
                    canRequestPermission = isPostNotificationsRuntimePermission &&
                        !state.permissionDenied,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenNotificationSettings,
                    modifier = Modifier.padding(horizontal = Spacing.Large),
                )
            }
        }

        if (!state.globalEnabled) {
            item(key = "disabled") {
                RemindersDisabledCard(
                    onEnable = { onGlobalEnabledChange(true) },
                    modifier = Modifier.padding(horizontal = Spacing.Large),
                )
            }
        }

        if (state.hasAnyRecords) {
            item(key = "filters") {
                ReminderFilterRow(selected = state.filter, onSelect = onFilterChange)
            }
        }

        when {
            !state.hasAnyRecords -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = stringResource(R.string.reminders_empty_title),
                    description = stringResource(R.string.reminders_empty_description),
                )
            }

            state.isEmpty -> item(key = "empty-filtered") {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = stringResource(R.string.reminders_empty_filtered_title),
                    description = stringResource(R.string.reminders_empty_filtered_description),
                )
            }

            else -> {
                if (state.upcoming.isNotEmpty()) {
                    item(key = "upcoming-header") {
                        SectionHeader(stringResource(R.string.reminders_upcoming_title))
                    }
                    items(items = state.upcoming, key = { "u-${it.sourceType}-${it.id}" }) { row ->
                        ReminderRow(
                            row = row,
                            // Global off keeps the list readable but honest: nothing here is armed.
                            enabled = state.remindersActive,
                            onClick = { onItemClick(row) },
                        )
                    }
                }

                if (state.inactive.isNotEmpty()) {
                    item(key = "inactive-header") {
                        SectionHeader(stringResource(R.string.reminders_inactive_title))
                    }
                    items(items = state.inactive, key = { "i-${it.sourceType}-${it.id}" }) { row ->
                        ReminderRow(row = row, enabled = false, onClick = { onItemClick(row) })
                    }
                }
            }
        }

        item(key = "defaults-divider") {
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))
        }

        item(key = "defaults") {
            ReminderDefaultsSection(defaults = state.defaults)
        }

        // Debug builds get a way to see a real notification without waiting for a date to come round.
        if (BuildConfig.DEBUG) {
            item(key = "debug") {
                DebugToolsSection(
                    onSendTestNotification = onSendTestNotification,
                    modifier = Modifier.padding(horizontal = Spacing.Large),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small),
    )
}

/**
 * Single select filters, split the way the reminder rules are split.
 *
 * Insurances and other charges are both obligations, but they follow different schedules, so telling
 * them apart here matches what the user is actually looking for.
 */
@Composable
private fun ReminderFilterRow(
    selected: ReminderFilter,
    onSelect: (ReminderFilter) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.Large),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        items(items = ReminderFilter.entries.toList(), key = { it.name }) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes())) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun NotificationRationaleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_permission_rationale_title)) },
        text = { Text(stringResource(R.string.reminders_permission_rationale_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reminders_permission_rationale_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private fun ReminderFilter.labelRes(): Int = when (this) {
    ReminderFilter.ALL -> R.string.filter_tag_all
    ReminderFilter.SUBSCRIPTIONS -> R.string.reminders_filter_subscriptions
    ReminderFilter.INSURANCE -> R.string.reminders_filter_insurance
    ReminderFilter.FEES -> R.string.reminders_filter_fees
}

@ThemePreviews
@Composable
private fun RemindersScreenPreview() {
    GriffThemePreview {
        RemindersScreen(
            state = RemindersUiState(isLoading = false, hasAnyRecords = false),
            message = null,
            onOpenDrawer = {},
            onGlobalEnabledChange = {},
            onFilterChange = {},
            onRequestPermission = {},
            onOpenNotificationSettings = {},
            onSendTestNotification = {},
            onItemClick = {},
            onMessageShown = {},
        )
    }
}
