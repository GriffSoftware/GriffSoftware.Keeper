package com.griff.subscriptions.application.obligation

import com.griff.subscriptions.application.fake.validatedObligationInput
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.model.PaymentStatus
import com.griff.subscriptions.domain.testing.FakeObligationRepository
import com.griff.subscriptions.domain.testing.FixedClockProvider
import com.griff.subscriptions.domain.testing.SequentialObligationIdGenerator
import com.griff.subscriptions.domain.testing.testObligation
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ObligationUseCasesTest {

    private val repository = FakeObligationRepository()
    private val clock = FixedClockProvider()

    private val add = AddObligationUseCase(
        repository = repository,
        idGenerator = SequentialObligationIdGenerator(),
        clock = clock,
    )
    private val update = UpdateObligationUseCase(repository, clock)
    private val delete = DeleteObligationUseCase(repository)
    private val get = GetObligationUseCase(repository)
    private val observeAll = ObserveObligationsUseCase(repository)
    private val observeOne = ObserveObligationUseCase(repository)

    @Test
    fun `adding stores every validated value and stamps the clock`() = runTest {
        val id = add(
            validatedObligationInput(
                name = "OC Ford",
                amount = "1240,00",
                paymentDate = LocalDate.of(2026, 3, 12),
                validUntil = LocalDate.of(2027, 3, 11),
                notes = "Polisa PZU",
            ),
        )

        val stored = repository.stored.single()
        assertEquals(ObligationId("obligation-1"), id)
        assertEquals("OC Ford", stored.name.value)
        assertEquals(124_000, stored.amount.minorUnits)
        assertEquals(PaymentState.Paid(LocalDate.of(2026, 3, 12)), stored.payment)
        assertEquals(LocalDate.of(2027, 3, 11), stored.validUntil)
        assertEquals("Polisa PZU", stored.notes)
        assertEquals(clock.now(), stored.createdAt)
        assertEquals(clock.now(), stored.updatedAt)
    }

    @Test
    fun `updating replaces the values and refreshes updatedAt only`() = runTest {
        val id = add(validatedObligationInput())
        val created = repository.stored.single().createdAt
        clock.advanceTo(Instant.parse("2026-09-01T10:00:00Z"))

        val result = update(
            id,
            validatedObligationInput(
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                amount = "320,00",
                paymentStatus = PaymentStatus.UNPAID,
                paymentDate = null,
                dueDate = LocalDate.of(2026, 9, 15),
                validUntil = null,
            ),
        )

        val stored = repository.stored.single()
        assertTrue(result.isSuccess)
        assertEquals("Podatek od gruntu", stored.name.value)
        assertEquals(ObligationCategory.LAND_TAX, stored.category)
        assertEquals(32_000, stored.amount.minorUnits)
        assertEquals(PaymentState.Unpaid, stored.payment)
        assertEquals(LocalDate.of(2026, 9, 15), stored.dueDate)
        assertNull(stored.validUntil)
        assertEquals(created, stored.createdAt)
        assertEquals(Instant.parse("2026-09-01T10:00:00Z"), stored.updatedAt)
    }

    @Test
    fun `updating an unknown id fails instead of creating a record`() = runTest {
        val result = update(ObligationId("missing"), validatedObligationInput())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ObligationNotFoundException)
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `deleting removes the record and an unknown id is a no-op`() = runTest {
        val id = add(validatedObligationInput())

        delete(id)
        delete(ObligationId("missing"))

        assertTrue(repository.stored.isEmpty())
        assertNull(get(id))
    }

    @Test
    fun `observing emits the stored records and then the deletion`() = runTest {
        val repository = FakeObligationRepository(listOf(testObligation(id = "o-1")))
        val observe = ObserveObligationsUseCase(repository)
        val observeSingle = ObserveObligationUseCase(repository)

        assertEquals(1, observe().first().size)
        assertEquals("OC Ford", observeSingle(ObligationId("o-1")).first()?.name?.value)

        repository.delete(ObligationId("o-1"))

        assertTrue(observe().first().isEmpty())
        assertNull(observeSingle(ObligationId("o-1")).first())
    }

    @Test
    fun `a one-shot read returns null for an unknown id`() = runTest {
        assertNull(get(ObligationId("missing")))
        assertTrue(observeAll().first().isEmpty())
        assertNull(observeOne(ObligationId("missing")).first())
    }
}
