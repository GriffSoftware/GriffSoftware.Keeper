package com.griff.keeper.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.keeper.application.reminder.ObserveReminderDashboardUseCase
import com.griff.keeper.application.reminder.ReminderDashboard
import com.griff.keeper.application.reminder.SendTestReminderUseCase
import com.griff.keeper.application.reminder.SetGlobalRemindersEnabledUseCase
import com.griff.keeper.domain.reminder.NotificationAvailability
import com.griff.keeper.domain.time.ClockProvider
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-off signals the reminders screen reacts to. */
sealed interface RemindersEvent {
    /**
     * The user asked for something that needs the Android notification permission.
     *
     * Raised only in response to a deliberate action - switching reminders on, or tapping the
     * warning - so the system dialog always arrives with a reason the user has just seen.
     */
    data object RequestNotificationPermission : RemindersEvent
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    observeDashboard: ObserveReminderDashboardUseCase,
    private val setGlobalEnabled: SetGlobalRemindersEnabledUseCase,
    private val sendTestReminder: SendTestReminderUseCase,
    private val notificationAvailability: NotificationAvailability,
    private val clock: ClockProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RemindersUiState(
            systemNotificationsEnabled = notificationAvailability.areNotificationsEnabled(),
        ),
    )
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RemindersEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<RemindersEvent> = _events.asSharedFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private var dashboard: ReminderDashboard = ReminderDashboard.Empty

    init {
        viewModelScope.launch {
            observeDashboard()
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { it.copy(isLoading = false) }
                    _message.value =
                        UiMessage(R.string.error_load_failed, severity = MessageSeverity.ERROR)
                }
                .collect { value ->
                    dashboard = value
                    _uiState.update { it.applied(value) }
                }
        }
    }

    /**
     * Re-reads the system state, which can change while the screen is open.
     *
     * The user can leave for the Android settings and come back, so the screen re-checks whenever it
     * is resumed instead of trusting what it saw when it was created.
     */
    fun onScreenResumed() {
        _uiState.update {
            it.copy(systemNotificationsEnabled = notificationAvailability.areNotificationsEnabled())
        }
    }

    fun onFilterChange(filter: ReminderFilter) {
        _uiState.update { it.copy(filter = filter).applied(dashboard) }
    }

    fun onGlobalEnabledChange(enabled: Boolean) {
        viewModelScope.launch { setGlobalEnabled(enabled) }
        // Switching reminders on is the moment the permission actually starts to matter, which makes
        // it the right - and only - moment to ask for it.
        if (enabled && !notificationAvailability.areNotificationsEnabled()) {
            _events.tryEmit(RemindersEvent.RequestNotificationPermission)
        }
    }

    fun onPermissionRequested() = _events.tryEmit(RemindersEvent.RequestNotificationPermission)

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                systemNotificationsEnabled = notificationAvailability.areNotificationsEnabled(),
                // Remembered so the screen can offer the system settings instead of a dialog that
                // Android will silently refuse to show a second time.
                permissionDenied = !granted,
            )
        }
        if (!granted) {
            _message.value = UiMessage(
                R.string.reminders_permission_denied,
                severity = MessageSeverity.WARNING,
            )
        }
    }

    /** Debug builds only; see the reminders screen. */
    fun onSendTestNotification() {
        viewModelScope.launch {
            runCatching { sendTestReminder() }
                .onSuccess {
                    _message.value = UiMessage(
                        R.string.reminders_debug_sent,
                        severity = MessageSeverity.SUCCESS,
                    )
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _message.value = UiMessage(
                        R.string.error_save_failed,
                        severity = MessageSeverity.ERROR,
                    )
                }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun RemindersUiState.applied(value: ReminderDashboard): RemindersUiState {
        val today = clock.today()
        return copy(
            isLoading = false,
            globalEnabled = value.globalEnabled,
            defaults = value.defaults,
            hasAnyRecords = value.items.isNotEmpty(),
            upcoming = value.upcoming.filter(filter::matches).map { it.toRow(today) },
            inactive = value.inactive.filter(filter::matches).map { it.toRow(today) },
        )
    }
}
