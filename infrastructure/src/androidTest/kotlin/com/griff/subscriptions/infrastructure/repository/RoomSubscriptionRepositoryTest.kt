package com.griff.subscriptions.infrastructure.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionId
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
class RoomSubscriptionRepositoryTest {

    private lateinit var database: GriffDatabase
    private lateinit var repository: RoomSubscriptionRepository

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GriffDatabase::class.java,
        ).build()
        repository = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun addedSubscriptionIsObservable() = runTest {
        val subscription = testSubscription(
            id = "id-1",
            name = "Spotify",
            priceMinorUnits = 3499,
            nextBillingDate = LocalDate.of(2026, 9, 14),
            managementUrl = "https://spotify.com/account",
        )

        repository.add(subscription)

        assertEquals(listOf(subscription), repository.observeAll().first())
        assertEquals(subscription, repository.findById(SubscriptionId("id-1")))
    }

    @Test
    fun subscriptionsAreOrderedByNameIgnoringCase() = runTest {
        repository.add(testSubscription(id = "1", name = "spotify"))
        repository.add(testSubscription(id = "2", name = "Netflix"))
        repository.add(testSubscription(id = "3", name = "Amazon Prime"))

        assertEquals(
            listOf("Amazon Prime", "Netflix", "spotify"),
            repository.observeAll().first().map { it.name.value },
        )
    }

    @Test
    fun updateReplacesStoredValues() = runTest {
        val original = testSubscription(id = "id-1", priceMinorUnits = 3499)
        repository.add(original)

        repository.update(
            original.copy(
                price = Money.ofUnits(39, 99),
                billingPeriod = BillingPeriod.YEARLY,
                nextBillingDate = LocalDate.of(2027, 1, 5),
            ),
        )

        val stored = repository.findById(SubscriptionId("id-1"))
        assertEquals(3999, stored?.price?.minorUnits)
        assertEquals(BillingPeriod.YEARLY, stored?.billingPeriod)
        assertEquals(LocalDate.of(2027, 1, 5), stored?.nextBillingDate)
    }

    @Test
    fun deleteRemovesTheRecord() = runTest {
        repository.add(testSubscription(id = "id-1"))

        repository.delete(SubscriptionId("id-1"))

        assertEquals(emptyList(), repository.observeAll().first())
        assertNull(repository.findById(SubscriptionId("id-1")))
        assertNull(repository.observeById(SubscriptionId("id-1")).first())
    }

    @Test
    fun deletingAnUnknownIdIsANoOp() = runTest {
        repository.add(testSubscription(id = "id-1"))

        repository.delete(SubscriptionId("missing"))

        assertEquals(1, repository.observeAll().first().size)
    }
}
