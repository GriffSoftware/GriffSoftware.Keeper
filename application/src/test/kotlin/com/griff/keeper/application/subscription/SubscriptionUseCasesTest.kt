package com.griff.keeper.application.subscription

import com.griff.keeper.domain.testing.FakeSubscriptionRepository
import com.griff.keeper.domain.testing.FixedClockProvider
import com.griff.keeper.domain.testing.SequentialIdGenerator
import com.griff.keeper.domain.testing.testSubscription
import com.griff.keeper.application.fake.validatedInput
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.SubscriptionId
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SubscriptionUseCasesTest {

    private val repository = FakeSubscriptionRepository()
    private val clock = FixedClockProvider()
    private val idGenerator = SequentialIdGenerator()

    private val addSubscription = AddSubscriptionUseCase(repository, idGenerator, clock)
    private val updateSubscription = UpdateSubscriptionUseCase(repository, clock)
    private val deleteSubscription = DeleteSubscriptionUseCase(repository)
    private val getSubscription = GetSubscriptionUseCase(repository)

    @Test
    fun `adds a subscription with generated id and timestamps`() = runTest {
        val id = addSubscription(
            validatedInput(price = "34,99", nextBillingDate = LocalDate.of(2026, 9, 14)),
        )

        val stored = repository.stored.single()
        assertEquals(SubscriptionId("id-1"), id)
        assertEquals("Spotify", stored.name.value)
        assertEquals(3499, stored.price.minorUnits)
        assertEquals(LocalDate.of(2026, 9, 14), stored.nextBillingDate)
        assertEquals(clock.now(), stored.createdAt)
        assertEquals(clock.now(), stored.updatedAt)
    }

    @Test
    fun `updates an existing subscription and bumps updatedAt only`() = runTest {
        val id = addSubscription(validatedInput())
        val created = repository.stored.single()
        clock.advanceTo(Instant.parse("2026-09-01T12:00:00Z"))

        val result = updateSubscription(
            id = id,
            input = validatedInput(price = "39,99", billingPeriod = BillingPeriod.YEARLY),
        )

        val updated = repository.stored.single()
        assertTrue(result.isSuccess)
        assertEquals(3999, updated.price.minorUnits)
        assertEquals(BillingPeriod.YEARLY, updated.billingPeriod)
        assertEquals(created.createdAt, updated.createdAt)
        assertEquals(Instant.parse("2026-09-01T12:00:00Z"), updated.updatedAt)
    }

    @Test
    fun `updating an unknown subscription fails`() = runTest {
        val result = updateSubscription(SubscriptionId("missing"), validatedInput())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SubscriptionNotFoundException)
    }

    @Test
    fun `deletes a subscription`() = runTest {
        val id = addSubscription(validatedInput())

        deleteSubscription(id)

        assertTrue(repository.stored.isEmpty())
        assertNull(getSubscription(id))
    }

    @Test
    fun `calculates totals of stored subscriptions`() {
        val totals = CalculateSubscriptionTotalsUseCase()(
            listOf(
                testSubscription(id = "1", priceMinorUnits = 3499),
                testSubscription(
                    id = "2",
                    priceMinorUnits = 129_900,
                    billingPeriod = BillingPeriod.YEARLY,
                ),
            ),
        )

        // 34,99 + (1299,00 / 12 = 108,25) = 143,24
        assertEquals(14_324, totals.monthly.minorUnits)
        // 419,88 + 1299,00 = 1718,88
        assertEquals(171_888, totals.yearly.minorUnits)
    }
}
