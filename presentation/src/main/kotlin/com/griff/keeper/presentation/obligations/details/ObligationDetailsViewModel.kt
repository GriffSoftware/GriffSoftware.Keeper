package com.griff.keeper.presentation.obligations.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.keeper.application.obligation.DeleteObligationUseCase
import com.griff.keeper.application.obligation.ObserveObligationUseCase
import com.griff.keeper.application.reminder.ObserveObligationReminderUseCase
import com.griff.keeper.application.reminder.SetObligationRemindersEnabledUseCase
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.ObligationId
import com.griff.keeper.domain.time.ClockProvider
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.navigation.OBLIGATION_ID_ARG
import com.griff.keeper.presentation.obligations.DeadlineStatus
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

/** One-off signals the obligation details screen reacts to. */
sealed interface ObligationDetailsEvent {
    data class Deleted(val name: String) : ObligationDetailsEvent
}

@HiltViewModel
class ObligationDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeObligation: ObserveObligationUseCase,
    private val deleteObligation: DeleteObligationUseCase,
    observeReminder: ObserveObligationReminderUseCase,
    private val setRemindersEnabled: SetObligationRemindersEnabledUseCase,
    private val clock: ClockProvider,
) : ViewModel() {

    private val obligationId = ObligationId(
        requireNotNull(savedStateHandle.get<String>(OBLIGATION_ID_ARG)) {
            "Missing $OBLIGATION_ID_ARG navigation argument"
        },
    )

    private val _uiState = MutableStateFlow(ObligationDetailsUiState())
    val uiState: StateFlow<ObligationDetailsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ObligationDetailsEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<ObligationDetailsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeObligation(obligationId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = UiMessage(
                                R.string.error_load_failed,
                                severity = MessageSeverity.ERROR,
                            ),
                        )
                    }
                }
                .collect { obligation ->
                    _uiState.update {
                        it.copy(isLoading = false, details = obligation?.toDetails())
                    }
                }
        }

        viewModelScope.launch {
            observeReminder(obligationId)
                .catch { throwable -> if (throwable is CancellationException) throw throwable }
                .collect { reminders -> _uiState.update { it.copy(reminders = reminders) } }
        }
    }

    /** Writes the record's own switch only; see the subscription details view model. */
    fun onRemindersEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            setRemindersEnabled(obligationId, enabled).onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        message = UiMessage(
                            R.string.error_save_failed,
                            severity = MessageSeverity.ERROR,
                        ),
                    )
                }
            }
        }
    }

    fun onDeleteRequest() = _uiState.update { it.copy(isDeleteDialogVisible = true) }

    fun onDeleteDismiss() = _uiState.update { it.copy(isDeleteDialogVisible = false) }

    fun onDeleteConfirm() {
        val name = _uiState.value.details?.name ?: return
        _uiState.update { it.copy(isDeleting = true, isDeleteDialogVisible = false) }
        viewModelScope.launch {
            runCatching { deleteObligation(obligationId) }
                .onSuccess { _events.tryEmit(ObligationDetailsEvent.Deleted(name)) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            message = UiMessage(
                                R.string.error_delete_failed,
                                severity = MessageSeverity.ERROR,
                            ),
                        )
                    }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun Obligation.toDetails() = ObligationDetails(
        id = id.value,
        name = name.value,
        category = category,
        amount = amount,
        paymentStatus = payment.status,
        paymentDate = paymentDate,
        dueDate = dueDate,
        validUntil = validUntil,
        notes = notes,
        deadline = DeadlineStatus.of(this, clock.today()),
    )
}
