package com.griff.keeper.application.reminder

import com.griff.keeper.application.provider.GetProviderUseCase
import com.griff.keeper.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.domain.reminder.ReminderKind
import com.griff.keeper.domain.reminder.ReminderSettings
import com.griff.keeper.domain.testing.FakeNotificationAvailability
import com.griff.keeper.domain.testing.FakeObligationRepository
import com.griff.keeper.domain.testing.FakeProviderCatalog
import com.griff.keeper.domain.testing.FakeReminderEventStore
import com.griff.keeper.domain.testing.FakeReminderSettingsRepository
import com.griff.keeper.domain.testing.FakeSubscriptionRepository
import com.griff.keeper.domain.testing.FixedClockProvider
import com.griff.keeper.domain.testing.RecordingReminderPublisher
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * The engine end to end, minus the platform.
 *
 * These are the guarantees a user would complain about if they broke: no duplicates, no reminders
 * for records they have switched off, no payment nagging after they have paid, and no silence about
 * a policy just because it has been paid for.
 */
class DeliverDueRemindersUseCaseTest {

    private val today = LocalDate.of(2026, 8, 21)
    private val clock = FixedClockProvider(instant = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())

    private val subscriptions = FakeSubscriptionRepository()
    private val obligations = FakeObligationRepository()
    private val settings = FakeReminderSettingsRepository()
    private val events = FakeReminderEventStore()
    private val availability = FakeNotificationAvailability()
    private val publisher = RecordingReminderPublisher()

    private val catalog = FakeProviderCatalog()
    private val factory = ReminderItemFactory(
        getProvider = GetProviderUseCase(catalog),
        getCategory = GetSubscriptionCategoryUseCase(catalog),
    )

    private val deliver = DeliverDueRemindersUseCase(
        subscriptions = subscriptions,
        obligations = obligations,
        settings = settings,
        events = events,
        availability = availability,
        publisher = publisher,
        factory = factory,
        clock = clock,
    )

    @Test
    fun `publishes the reminder that falls on today`() = runTest {
        subscriptions.add(testSubscription(name = "Netflix", nextBillingDate = today.plusDays(7)))

        val published = deliver()

        assertEquals(1, published)
        assertEquals("Netflix", publisher.published.single().title)
        assertEquals(
            ReminderKind.SUBSCRIPTION_RENEWAL,
            publisher.published.single().occurrence.kind,
        )
    }

    @Test
    fun `running twice on the same day publishes once`() = runTest {
        obligations.add(
            testObligation(name = "OC Ford", validUntil = today.plusDays(30)),
        )

        deliver()
        deliver()

        assertEquals(1, publisher.published.size)
    }

    @Test
    fun `a record with its own switch off stays silent`() = runTest {
        subscriptions.add(
            testSubscription(nextBillingDate = today.plusDays(7), remindersEnabled = false),
        )

        assertEquals(0, deliver())
        assertTrue(publisher.published.isEmpty())
    }

    @Test
    fun `the app wide switch silences everything without marking anything as delivered`() = runTest {
        subscriptions.add(testSubscription(nextBillingDate = today.plusDays(7)))
        settings.setGlobalEnabled(false)

        assertEquals(0, deliver())
        // Nothing was shown, so nothing may be recorded - otherwise the reminder would be lost for
        // good once the user switches reminders back on.
        assertTrue(events.keys.isEmpty())
    }

    @Test
    fun `a system that blocks notifications suppresses delivery without consuming it`() = runTest {
        subscriptions.add(testSubscription(nextBillingDate = today.plusDays(7)))
        availability.enabled = false

        assertEquals(0, deliver())
        assertTrue(events.keys.isEmpty())

        availability.enabled = true
        assertEquals(1, deliver())
    }

    @Test
    fun `a settled charge produces no payment reminder`() = runTest {
        obligations.add(
            testObligation(
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                payment = PaymentState.Paid(today),
                dueDate = today.plusDays(7),
                validUntil = null,
            ),
        )

        assertEquals(0, deliver())
    }

    @Test
    fun `an open charge is reminded about seven days before its deadline`() = runTest {
        obligations.add(
            testObligation(
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                payment = PaymentState.Unpaid,
                dueDate = today.plusDays(7),
                validUntil = null,
            ),
        )

        assertEquals(1, deliver())
        assertEquals(ReminderKind.PAYMENT_DUE, publisher.published.single().occurrence.kind)
    }

    @Test
    fun `a paid policy is still reminded about its expiry`() = runTest {
        obligations.add(
            testObligation(
                name = "OC Ford",
                category = ObligationCategory.VEHICLE_INSURANCE,
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
                validUntil = today.plusDays(30),
            ),
        )

        assertEquals(1, deliver())
        assertEquals(ReminderKind.INSURANCE_EXPIRY, publisher.published.single().occurrence.kind)
    }

    @Test
    fun `moving the target date reopens the reminders for the new cycle`() = runTest {
        val original = testSubscription(name = "Netflix", nextBillingDate = today.plusDays(7))
        subscriptions.add(original)
        deliver()
        assertEquals(1, publisher.published.size)

        // Renewed a month later: the August reminder must not silence the September one.
        subscriptions.update(original.copy(nextBillingDate = today.plusDays(37)))
        clock.advanceTo(today.plusDays(30).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())

        assertEquals(1, deliver())
        assertEquals(
            today.plusDays(37),
            publisher.published.last().occurrence.targetDate,
        )
    }

    @Test
    fun `a deleted record cannot produce a reminder`() = runTest {
        val subscription = testSubscription(nextBillingDate = today.plusDays(7))
        subscriptions.add(subscription)
        subscriptions.delete(subscription.id)

        assertEquals(0, deliver())
    }

    @Test
    fun `reminders that fell before today are never delivered late`() = runTest {
        // Installed after the deadline had already passed: the user must not be handed a month of
        // history at once.
        obligations.add(testObligation(validUntil = today.minusDays(1)))
        subscriptions.add(testSubscription(nextBillingDate = today.minusDays(3)))

        assertEquals(0, deliver())
    }

    @Test
    fun `old bookkeeping is pruned, recent bookkeeping is kept`() = runTest {
        events.markDelivered("stale", Instant.parse("2020-01-01T00:00:00Z"))
        events.markDelivered("fresh", clock.now())

        deliver()

        assertEquals(setOf("fresh"), events.keys)
    }

    @Test
    fun `three reminders on the same day are all published`() = runTest {
        subscriptions.add(testSubscription(name = "Netflix", nextBillingDate = today.plusDays(7)))
        obligations.add(
            testObligation(name = "OC Ford", validUntil = today.plusDays(30)),
        )
        obligations.add(
            testObligation(
                id = "obligation-2",
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                payment = PaymentState.Unpaid,
                dueDate = today.plusDays(7),
                validUntil = null,
            ),
        )

        assertEquals(3, deliver())
        assertEquals(3, publisher.published.map { it.occurrence.notificationId }.distinct().size)
    }

    @Test
    fun `settings keep their defaults out of the box`() = runTest {
        assertEquals(ReminderSettings.Default, settings.current())
    }
}
