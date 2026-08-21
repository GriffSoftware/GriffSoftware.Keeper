package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.testing.FakeObligationRepository
import com.griff.subscriptions.domain.testing.FakeProviderCatalog
import com.griff.subscriptions.domain.testing.FakeReminderEventStore
import com.griff.subscriptions.domain.testing.FakeReminderSettingsRepository
import com.griff.subscriptions.domain.testing.FakeSubscriptionRepository
import com.griff.subscriptions.domain.testing.FixedClockProvider
import com.griff.subscriptions.domain.testing.testObligation
import com.griff.subscriptions.domain.testing.testSubscription
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * What the reminders screen is told, which is a different question from what gets delivered.
 *
 * The screen has to stay useful when reminders are off and honest when a record cannot be scheduled,
 * so the dashboard reports every record together with the reason it is quiet.
 */
class ReminderDashboardTest {

    private val today = LocalDate.of(2026, 8, 21)
    private val clock = FixedClockProvider(instant = today.atStartOfDay(ZoneOffset.UTC).toInstant())

    private val subscriptions = FakeSubscriptionRepository()
    private val obligations = FakeObligationRepository()
    private val settings = FakeReminderSettingsRepository()
    private val events = FakeReminderEventStore()

    private val catalog = FakeProviderCatalog()
    private val factory = ReminderItemFactory(
        getProvider = GetProviderUseCase(catalog),
        getCategory = GetSubscriptionCategoryUseCase(catalog),
    )

    private val observeDashboard = ObserveReminderDashboardUseCase(
        subscriptions = subscriptions,
        obligations = obligations,
        settings = settings,
        events = events,
        factory = factory,
        clock = clock,
    )

    @Test
    fun `the most urgent reminder comes first`() = runTest {
        subscriptions.add(
            testSubscription(id = "s-1", name = "Google Workspace", nextBillingDate = today.plusDays(25)),
        )
        subscriptions.add(
            testSubscription(id = "s-2", name = "Netflix", nextBillingDate = today.plusDays(7)),
        )

        val upcoming = observeDashboard().first().upcoming

        assertEquals(listOf("Netflix", "Google Workspace"), upcoming.map { it.title })
        assertEquals(today, upcoming.first().nextReminder?.fireDate)
    }

    @Test
    fun `a subscription without a renewal date is listed as having no date`() = runTest {
        subscriptions.add(testSubscription(name = "Netflix", nextBillingDate = null))

        val dashboard = observeDashboard().first()

        assertTrue(dashboard.upcoming.isEmpty())
        assertEquals(ReminderItemStatus.NO_DATE, dashboard.inactive.single().status)
        assertNull(dashboard.inactive.single().targetDate)
    }

    @Test
    fun `a record the user switched off is listed as disabled, not hidden`() = runTest {
        subscriptions.add(
            testSubscription(nextBillingDate = today.plusDays(7), remindersEnabled = false),
        )

        val dashboard = observeDashboard().first()

        assertEquals(ReminderItemStatus.DISABLED, dashboard.inactive.single().status)
    }

    @Test
    fun `a date whose reminders have all passed is listed as passed`() = runTest {
        subscriptions.add(testSubscription(nextBillingDate = today.minusDays(1)))

        assertEquals(ReminderItemStatus.PASSED, observeDashboard().first().inactive.single().status)
    }

    @Test
    fun `the list survives the app wide switch being turned off`() = runTest {
        subscriptions.add(testSubscription(nextBillingDate = today.plusDays(7)))
        settings.setGlobalEnabled(false)

        val dashboard = observeDashboard().first()

        // The screen shows what would happen once reminders are switched back on rather than an
        // empty page.
        assertEquals(false, dashboard.globalEnabled)
        assertEquals(1, dashboard.upcoming.size)
    }

    @Test
    fun `an already delivered reminder is replaced by the next one`() = runTest {
        obligations.add(
            testObligation(
                id = "oc",
                name = "OC Ford",
                category = ObligationCategory.VEHICLE_INSURANCE,
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
                validUntil = LocalDate.of(2026, 9, 20),
            ),
        )
        events.markDelivered("OBLIGATION:oc:2026-09-20:30", clock.now())

        val item = observeDashboard().first().upcoming.single()

        assertEquals(LocalDate.of(2026, 9, 13), item.nextReminder?.fireDate)
        assertEquals(7, item.nextReminder?.daysBefore)
    }

    @Test
    fun `turning the app wide switch off leaves the per record switches untouched`() = runTest {
        subscriptions.add(
            testSubscription(id = "s-1", name = "Netflix", nextBillingDate = today.plusDays(7), remindersEnabled = false),
        )
        subscriptions.add(
            testSubscription(id = "s-2", name = "Google Workspace", nextBillingDate = today.plusDays(7)),
        )
        obligations.add(testObligation(name = "OC Ford", validUntil = today.plusDays(30)))

        SetGlobalRemindersEnabledUseCase(settings)(false)
        SetGlobalRemindersEnabledUseCase(settings)(true)

        val dashboard = observeDashboard().first()

        assertEquals(true, dashboard.globalEnabled)
        assertEquals(
            mapOf("Netflix" to false, "Google Workspace" to true, "OC Ford" to true),
            dashboard.items.associate { it.title to it.remindersEnabled },
        )
    }

    @Test
    fun `switching one record off does not touch the others`() = runTest {
        val netflix = testSubscription(id = "s-1", name = "Netflix", nextBillingDate = today.plusDays(7))
        val workspace = testSubscription(id = "s-2", name = "Google Workspace", nextBillingDate = today.plusDays(7))
        subscriptions.add(netflix)
        subscriptions.add(workspace)

        SetSubscriptionRemindersEnabledUseCase(subscriptions, clock)(netflix.id, false)

        val flags = observeDashboard().first().items.associate { it.title to it.remindersEnabled }
        assertEquals(mapOf("Netflix" to false, "Google Workspace" to true), flags)
    }
}
