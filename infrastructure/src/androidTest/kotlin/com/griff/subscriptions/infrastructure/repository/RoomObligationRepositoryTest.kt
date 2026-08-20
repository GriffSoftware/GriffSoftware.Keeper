package com.griff.subscriptions.infrastructure.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.testing.testObligation
import com.griff.subscriptions.domain.testing.testSubscription
import com.griff.subscriptions.infrastructure.database.GriffDatabase
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

/** Verifies the repository against a real, in-memory Room database. */
@RunWith(AndroidJUnit4::class)
class RoomObligationRepositoryTest {

    private lateinit var database: GriffDatabase
    private lateinit var repository: RoomObligationRepository

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GriffDatabase::class.java,
        ).build()
        repository = RoomObligationRepository(database.obligationDao(), Dispatchers.IO)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun addedObligationIsObservableWithEveryField() = runTest {
        val obligation = testObligation(
            id = "o-1",
            name = "OC Ford",
            amountMinorUnits = 124_000,
            payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
            validUntil = LocalDate.of(2027, 3, 11),
            notes = "Polisa PZU nr ABC123",
        )

        repository.add(obligation)

        assertEquals(listOf(obligation), repository.observeAll().first())
        assertEquals(obligation, repository.findById(ObligationId("o-1")))
    }

    @Test
    fun unpaidObligationsKeepTheirDueDateAndNoPaymentDate() = runTest {
        val obligation = testObligation(
            id = "o-1",
            name = "Podatek od nieruchomości",
            category = ObligationCategory.PROPERTY_TAX,
            payment = PaymentState.Unpaid,
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
        )

        repository.add(obligation)

        val stored = repository.findById(ObligationId("o-1"))
        assertEquals(PaymentState.Unpaid, stored?.payment)
        assertEquals(LocalDate.of(2026, 9, 15), stored?.dueDate)
        assertNull(stored?.paymentDate)
        assertNull(stored?.validUntil)
    }

    @Test
    fun updateReplacesStoredValues() = runTest {
        val original = testObligation(id = "o-1", payment = PaymentState.Unpaid, validUntil = null)
        repository.add(original)

        repository.update(
            original.copy(
                amount = Money.ofUnits(1_240, 50),
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
                validUntil = LocalDate.of(2027, 3, 11),
                notes = "Zapłacone przelewem",
            ),
        )

        val stored = repository.findById(ObligationId("o-1"))
        assertEquals(124_050, stored?.amount?.minorUnits)
        assertEquals(LocalDate.of(2026, 3, 12), stored?.paymentDate)
        assertEquals(LocalDate.of(2027, 3, 11), stored?.validUntil)
        assertEquals("Zapłacone przelewem", stored?.notes)
    }

    @Test
    fun deleteRemovesTheRecord() = runTest {
        repository.add(testObligation(id = "o-1"))

        repository.delete(ObligationId("o-1"))

        assertEquals(emptyList(), repository.observeAll().first())
        assertNull(repository.findById(ObligationId("o-1")))
        assertNull(repository.observeById(ObligationId("o-1")).first())
    }

    @Test
    fun deletingAnUnknownIdIsANoOp() = runTest {
        repository.add(testObligation(id = "o-1"))

        repository.delete(ObligationId("missing"))

        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun obligationsAndSubscriptionsShareTheDatabaseWithoutInterfering() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        subscriptions.add(testSubscription(id = "s-1"))
        repository.add(testObligation(id = "o-1"))

        assertEquals(1, subscriptions.observeAll().first().size)
        assertEquals(1, repository.observeAll().first().size)
    }
}
