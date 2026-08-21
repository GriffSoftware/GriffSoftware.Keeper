package com.griff.subscriptions.presentation.reminders

import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.reminder.ObserveReminderDashboardUseCase
import com.griff.subscriptions.application.reminder.ReminderItemFactory
import com.griff.subscriptions.application.reminder.SendTestReminderUseCase
import com.griff.subscriptions.application.reminder.SetGlobalRemindersEnabledUseCase
import com.griff.subscriptions.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.testing.FakeNotificationAvailability
import com.griff.subscriptions.domain.testing.FakeObligationRepository
import com.griff.subscriptions.domain.testing.FakeProviderCatalog
import com.griff.subscriptions.domain.testing.FakeReminderEventStore
import com.griff.subscriptions.domain.testing.FakeReminderSettingsRepository
import com.griff.subscriptions.domain.testing.FakeSubscriptionRepository
import com.griff.subscriptions.domain.testing.FixedClockProvider
import com.griff.subscriptions.domain.testing.RecordingReminderPublisher
import com.griff.subscriptions.domain.testing.testObligation
import com.griff.subscriptions.domain.testing.testSubscription
import com.griff.subscriptions.presentation.util.MainDispatcherRule
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class RemindersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 8, 21)
    private val clock = FixedClockProvider(instant = today.atStartOfDay(ZoneOffset.UTC).toInstant())

    private val catalog = FakeProviderCatalog()
    private val subscriptions = FakeSubscriptionRepository(
        listOf(
            testSubscription(
                id = "s-1",
                providerId = "netflix",
                name = "Netflix",
                nextBillingDate = today.plusDays(7),
            ),
            testSubscription(
                id = "s-2",
                providerId = "spotify",
                name = "Spotify",
                nextBillingDate = null,
            ),
        ),
    )
    private val obligations = FakeObligationRepository(
        listOf(
            testObligation(
                id = "o-1",
                name = "OC Ford",
                category = ObligationCategory.VEHICLE_INSURANCE,
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
                validUntil = today.plusDays(30),
            ),
            testObligation(
                id = "o-2",
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                payment = PaymentState.Unpaid,
                dueDate = today.plusDays(7),
                validUntil = null,
            ),
        ),
    )
    private val settings = FakeReminderSettingsRepository()
    private val events = FakeReminderEventStore()
    private val availability = FakeNotificationAvailability()
    private val publisher = RecordingReminderPublisher()

    private fun viewModel(): RemindersViewModel {
        val factory = ReminderItemFactory(
            getProvider = GetProviderUseCase(catalog),
            getCategory = GetSubscriptionCategoryUseCase(catalog),
        )
        return RemindersViewModel(
            observeDashboard = ObserveReminderDashboardUseCase(
                subscriptions = subscriptions,
                obligations = obligations,
                settings = settings,
                events = events,
                factory = factory,
                clock = clock,
            ),
            setGlobalEnabled = SetGlobalRemindersEnabledUseCase(settings),
            sendTestReminder = SendTestReminderUseCase(
                subscriptions = subscriptions,
                obligations = obligations,
                publisher = publisher,
                clock = clock,
            ),
            notificationAvailability = availability,
            clock = clock,
        )
    }

    @Test
    fun `lists the records that have a reminder ahead of them, most urgent first`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        val state = model.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Netflix", "OC Ford", "Podatek od gruntu"), state.upcoming.map { it.title })
        assertEquals(listOf("Spotify"), state.inactive.map { it.title })
    }

    @Test
    fun `the filter narrows the list to one source`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        model.onFilterChange(ReminderFilter.INSURANCE)
        assertEquals(listOf("OC Ford"), model.uiState.value.upcoming.map { it.title })

        model.onFilterChange(ReminderFilter.FEES)
        assertEquals(listOf("Podatek od gruntu"), model.uiState.value.upcoming.map { it.title })

        model.onFilterChange(ReminderFilter.SUBSCRIPTIONS)
        assertEquals(listOf("Netflix"), model.uiState.value.upcoming.map { it.title })
    }

    @Test
    fun `every row says how many days are left before it speaks`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        val netflix = model.uiState.value.upcoming.first { it.title == "Netflix" }
        assertEquals(today, netflix.nextReminderDate)
        assertEquals(0L, netflix.daysUntilReminder)
        assertEquals(7L, netflix.daysUntilTarget)
    }

    @Test
    fun `turning reminders off keeps the list but reports them as inactive`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        model.onGlobalEnabledChange(false)
        advanceUntilIdle()

        val state = model.uiState.value
        assertFalse(state.globalEnabled)
        assertFalse(state.remindersActive)
        assertEquals(3, state.upcoming.size)
    }

    @Test
    fun `a blocked system is reported separately from the app switch`() = runTest {
        availability.enabled = false
        val model = viewModel()
        advanceUntilIdle()

        val state = model.uiState.value
        assertTrue(state.globalEnabled)
        assertFalse(state.systemNotificationsEnabled)
        assertTrue(state.isBlockedBySystem)
        assertFalse(state.remindersActive)
    }

    @Test
    fun `switching reminders on without permission asks for it`() = runTest {
        availability.enabled = false
        val model = viewModel()
        advanceUntilIdle()

        var requested = false
        val job = launch { model.events.collect { requested = true } }
        advanceUntilIdle()

        model.onGlobalEnabledChange(true)
        advanceUntilIdle()

        assertTrue(requested)
        job.cancel()
    }

    @Test
    fun `resuming picks up a permission granted outside the app`() = runTest {
        availability.enabled = false
        val model = viewModel()
        advanceUntilIdle()
        assertFalse(model.uiState.value.systemNotificationsEnabled)

        availability.enabled = true
        model.onScreenResumed()

        assertTrue(model.uiState.value.systemNotificationsEnabled)
    }

    @Test
    fun `the debug tool publishes a real reminder`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        model.onSendTestNotification()
        advanceUntilIdle()

        assertEquals(1, publisher.published.size)
    }
}
