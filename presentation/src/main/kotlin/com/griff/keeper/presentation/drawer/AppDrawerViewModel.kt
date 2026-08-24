package com.griff.keeper.presentation.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.application.appinfo.GetAppVersionUseCase
import com.griff.keeper.application.reminder.ObserveReminderDashboardUseCase
import com.griff.keeper.application.subscription.CalculateSubscriptionTotalsUseCase
import com.griff.keeper.application.subscription.ObserveSubscriptionsUseCase
import com.griff.keeper.domain.model.SubscriptionTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Supplies the drawer with the real version of the running build and its header figures. */
@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    getAppVersion: GetAppVersionUseCase,
    observeSubscriptions: ObserveSubscriptionsUseCase,
    calculateTotals: CalculateSubscriptionTotalsUseCase,
    observeReminderDashboard: ObserveReminderDashboardUseCase,
) : ViewModel() {

    val appVersion: AppVersion = getAppVersion()

    val totals: StateFlow<SubscriptionTotals> = observeSubscriptions()
        .map(calculateTotals::invoke)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SubscriptionTotals.Empty)

    /** How many reminders are armed right now, shown as the badge on the drawer's reminders row. */
    val upcomingReminderCount: StateFlow<Int> = observeReminderDashboard()
        .map { it.upcoming.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
