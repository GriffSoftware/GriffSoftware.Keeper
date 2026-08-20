package com.griff.subscriptions.presentation.obligations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.subscriptions.application.obligation.ObligationSearchResult
import com.griff.subscriptions.application.obligation.SearchObligationsUseCase
import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.search.ObligationFilter
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.MessageSeverity
import com.griff.subscriptions.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class ObligationsViewModel @Inject constructor(
    searchObligations: SearchObligationsUseCase,
    private val clock: ClockProvider,
) : ViewModel() {

    /**
     * Period, search text and tag in one value.
     *
     * A single filter means the three controls cannot get out of step with each other, and the list
     * is recomputed exactly once per change.
     */
    private val filter = MutableStateFlow(
        ObligationFilter(period = ExpensePeriod.currentYear(clock.today())),
    )

    val uiState: StateFlow<ObligationsUiState> = searchObligations(filter.asStateFlow())
        .map { result -> result.toUiState() }
        .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(
                ObligationsUiState(
                    period = filter.value.period,
                    today = clock.today(),
                    isLoading = false,
                    message = UiMessage(
                        R.string.error_load_failed,
                        severity = MessageSeverity.ERROR,
                    ),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ObligationsUiState(
                period = filter.value.period,
                today = clock.today(),
            ),
        )

    fun onQueryChange(value: String) = filter.update { it.copy(query = value) }

    fun onTagChange(value: ObligationTag?) = filter.update { it.copy(tag = value) }

    fun onPeriodChange(value: ExpensePeriod) = filter.update { it.copy(period = value) }

    private fun ObligationSearchResult.toUiState(): ObligationsUiState {
        val today = clock.today()
        return ObligationsUiState(
            period = filter.period,
            today = today,
            isLoading = false,
            query = filter.query,
            selectedTag = filter.tag,
            availableTags = availableTags,
            items = matching.map { it.toListItem(today) },
            totals = totals,
            totalCount = totalCount,
        )
    }

    private fun Obligation.toListItem(today: LocalDate) = ObligationListItem(
        id = id.value,
        name = name.value,
        category = category,
        amount = amount,
        isPaid = isPaid,
        paymentDate = paymentDate,
        dueDate = dueDate,
        validUntil = validUntil,
        deadline = DeadlineStatus.of(this, today),
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
