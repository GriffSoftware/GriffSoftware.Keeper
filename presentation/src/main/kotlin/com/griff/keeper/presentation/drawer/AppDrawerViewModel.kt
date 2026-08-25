package com.griff.keeper.presentation.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.application.appinfo.GetAppVersionUseCase
import com.griff.keeper.application.currency.BeginCurrencyChangeUseCase
import com.griff.keeper.application.currency.ChangeAppCurrencyUseCase
import com.griff.keeper.application.currency.CurrencyChangeOutcome
import com.griff.keeper.application.currency.ObserveAppCurrencyUseCase
import com.griff.keeper.application.currency.PreviewCurrencyConversionUseCase
import com.griff.keeper.application.reminder.ObserveReminderDashboardUseCase
import com.griff.keeper.application.subscription.CalculateSubscriptionTotalsUseCase
import com.griff.keeper.application.subscription.ObserveSubscriptionsUseCase
import com.griff.keeper.domain.currency.CurrencyConversionErrorType
import com.griff.keeper.domain.currency.CurrencyConversionException
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.domain.validation.ExchangeRateParseResult
import com.griff.keeper.domain.validation.ExchangeRateParser
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.currency.CurrencyChangeStep
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Supplies the drawer with the real version of the running build, its header figures, and drives the
 * currency-change flow reachable from it.
 *
 * The currency flow lives here rather than in a screen-scoped ViewModel because the drawer is the one
 * place it is reachable from, on every screen, exactly like the language picker already is.
 */
@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    getAppVersion: GetAppVersionUseCase,
    observeSubscriptions: ObserveSubscriptionsUseCase,
    calculateTotals: CalculateSubscriptionTotalsUseCase,
    observeReminderDashboard: ObserveReminderDashboardUseCase,
    observeAppCurrency: ObserveAppCurrencyUseCase,
    private val beginCurrencyChange: BeginCurrencyChangeUseCase,
    private val previewCurrencyConversion: PreviewCurrencyConversionUseCase,
    private val changeAppCurrency: ChangeAppCurrencyUseCase,
) : ViewModel() {

    val appVersion: AppVersion = getAppVersion()

    val totals: StateFlow<SubscriptionTotals> = observeSubscriptions()
        .map(calculateTotals::invoke)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SubscriptionTotals.Empty)

    /** How many reminders are armed right now, shown as the badge on the drawer's reminders row. */
    val upcomingReminderCount: StateFlow<Int> = observeReminderDashboard()
        .map { it.upcoming.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    val appCurrency: StateFlow<Currency> = observeAppCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Currency.Default)

    private val _currencyChangeStep = MutableStateFlow<CurrencyChangeStep>(CurrencyChangeStep.None)
    internal val currencyChangeStep: StateFlow<CurrencyChangeStep> = _currencyChangeStep.asStateFlow()

    /** Transient feedback for the currency flow: success or failure, shown as a snackbar. */
    private val _currencyMessage = MutableStateFlow<UiMessage?>(null)
    val currencyMessage: StateFlow<UiMessage?> = _currencyMessage.asStateFlow()

    fun onCurrencyMessageShown() = _currencyMessage.update { null }

    /** Picked from [com.griff.keeper.presentation.common.currency.CurrencyPickerDialog]. */
    fun onCurrencySelected(target: Currency) {
        if (_currencyChangeStep.value != CurrencyChangeStep.None) return
        viewModelScope.launch {
            when (val outcome = beginCurrencyChange(target)) {
                is CurrencyChangeOutcome.SwitchedImmediately ->
                    _currencyMessage.value = currencyChangedMessage(outcome.currency)

                is CurrencyChangeOutcome.RequiresConversion ->
                    _currencyChangeStep.value = CurrencyChangeStep.EnteringRate(outcome.from, outcome.to)
            }
        }
    }

    fun onRateInputChanged(raw: String) {
        val step = _currencyChangeStep.value as? CurrencyChangeStep.EnteringRate ?: return
        _currencyChangeStep.value = step.copy(rateInput = raw, error = null)
    }

    /** "Dalej" on the rate dialog: parses the rate and, if valid, moves to the preview. */
    fun onRateConfirmed() {
        val step = _currencyChangeStep.value as? CurrencyChangeStep.EnteringRate ?: return
        when (val result = ExchangeRateParser.parse(step.rateInput)) {
            is ExchangeRateParseResult.Failure ->
                _currencyChangeStep.value = step.copy(error = result.error)

            is ExchangeRateParseResult.Success -> viewModelScope.launch {
                val preview = previewCurrencyConversion(step.from, step.to, result.rate)
                // Nothing else may have changed the step while the preview was being built, but a fast
                // double tap of Cancel is still possible; only advance if the rate step is still there.
                if (_currencyChangeStep.value == step) {
                    _currencyChangeStep.value = CurrencyChangeStep.Previewing(preview)
                }
            }
        }
    }

    /** "Dalej" on the preview: moves to the final, explicit confirmation. */
    fun onPreviewConfirmed() {
        val step = _currencyChangeStep.value as? CurrencyChangeStep.Previewing ?: return
        _currencyChangeStep.value = CurrencyChangeStep.Confirming(step.preview)
    }

    /** "Przelicz" on the final confirmation: runs the transactional conversion. */
    fun onConversionConfirmed() {
        val step = _currencyChangeStep.value as? CurrencyChangeStep.Confirming ?: return
        val preview = step.preview
        _currencyChangeStep.value = CurrencyChangeStep.Converting(preview.from, preview.to)

        viewModelScope.launch {
            changeAppCurrency(preview.from, preview.to, preview.rate)
                .onSuccess {
                    _currencyChangeStep.value = CurrencyChangeStep.None
                    _currencyMessage.value = currencyChangedMessage(preview.to)
                }
                .onFailure { throwable ->
                    _currencyChangeStep.value = CurrencyChangeStep.None
                    _currencyMessage.value = currencyChangeFailedMessage(throwable)
                }
        }
    }

    /** Cancel at any non-converting step: a clean abort, nothing has been written. */
    fun onCurrencyChangeCancelled() {
        if (_currencyChangeStep.value is CurrencyChangeStep.Converting) return
        _currencyChangeStep.value = CurrencyChangeStep.None
    }

    private fun currencyChangedMessage(currency: Currency) = UiMessage(
        textRes = R.string.currency_change_success,
        formatArgs = listOf(currency.code),
        severity = MessageSeverity.SUCCESS,
    )

    private fun currencyChangeFailedMessage(throwable: Throwable): UiMessage {
        val errorType = (throwable as? CurrencyConversionException)?.errorType
        val textRes = if (errorType == CurrencyConversionErrorType.PREFERENCE_NOT_PERSISTED) {
            R.string.currency_change_error_preference_not_saved
        } else {
            R.string.currency_change_error
        }
        return UiMessage(textRes = textRes, severity = MessageSeverity.ERROR)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
