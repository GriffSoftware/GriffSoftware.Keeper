package com.griff.keeper.infrastructure.repository

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import com.griff.keeper.infrastructure.database.GriffDatabase
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

/**
 * Verifies the global currency conversion against a real Room database.
 *
 * Like [RoomBackupImportRepositoryTest], the all-or-nothing guarantee is the reason this runs at a
 * device level: an in-memory double can be made to look transactional, whereas only a real database
 * can show that it is.
 */
@RunWith(AndroidJUnit4::class)
class RoomCurrencyConversionRepositoryTest {

    private lateinit var database: GriffDatabase
    private lateinit var repository: RoomCurrencyConversionRepository
    private val rate = ExchangeRate.ofOrNull(BigDecimal("4.25")) ?: error("test rate must be valid")

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GriffDatabase::class.java,
        ).build()
        repository = RoomCurrencyConversionRepository(database, Dispatchers.IO)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun everySubscriptionAndObligationIsConverted() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        val obligations = RoomObligationRepository(database.obligationDao(), Dispatchers.IO)
        subscriptions.add(testSubscription(id = "s1", priceMinorUnits = 5_999, currency = Currency.PLN))
        subscriptions.add(testSubscription(id = "s2", priceMinorUnits = 120_000, currency = Currency.PLN))
        obligations.add(testObligation(id = "o1", amountMinorUnits = 120_000, currency = Currency.PLN))

        repository.convertAll(Currency.PLN, Currency.EUR, rate)

        val convertedSubscriptions = subscriptions.observeAll().first().associateBy { it.id.value }
        assertTrue(convertedSubscriptions.values.all { it.currency == Currency.EUR })
        // 59.99 / 4.25 = 14.1152... -> 14.12
        assertEquals(Money.ofUnits(14, 12), convertedSubscriptions.getValue("s1").price)

        val convertedObligations = obligations.observeAll().first()
        assertTrue(convertedObligations.all { it.currency == Currency.EUR })
    }

    @Test
    fun emptyDatabaseConversionIsANoOp() = runTest {
        repository.convertAll(Currency.PLN, Currency.EUR, rate)

        assertEquals(0, database.subscriptionDao().count())
        assertEquals(0, database.obligationDao().count())
    }

    @Test
    fun aFailureHalfwayThroughLeavesEveryRecordInTheOriginalCurrency() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        repeat(3) { subscriptions.add(testSubscription(id = "local-$it", currency = Currency.PLN)) }
        val before = subscriptions.observeAll().first()

        // The exact shape of a conversion that dies after subscriptions are rewritten but before
        // obligations are - the failure mode the transaction exists to make impossible.
        assertFailsWith<IllegalStateException> {
            database.withTransaction {
                database.subscriptionDao().upsertAll(
                    database.subscriptionDao().getAll().map { it.copy(currencyCode = "EUR") },
                )
                error("Simulated failure halfway through the conversion")
            }
        }

        assertEquals(before, subscriptions.observeAll().first())
        assertTrue(subscriptions.observeAll().first().all { it.currency == Currency.PLN })
    }

    @Test
    fun convertingBackAndForthRoundsButNeverCorruptsTheCurrencyColumn() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        subscriptions.add(testSubscription(id = "s1", priceMinorUnits = 5_999, currency = Currency.PLN))

        repository.convertAll(Currency.PLN, Currency.EUR, rate)
        repository.convertAll(Currency.EUR, Currency.PLN, rate)

        val restored = subscriptions.observeAll().first().single()
        assertEquals(Currency.PLN, restored.currency)
        // Rounding through EUR and back need not reproduce the exact original minor units.
        assertTrue(kotlin.math.abs(restored.price.minorUnits - 5_999) <= 2)
    }
}
