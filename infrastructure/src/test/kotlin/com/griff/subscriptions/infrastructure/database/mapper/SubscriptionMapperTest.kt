package com.griff.subscriptions.infrastructure.database.mapper

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.ManagementUrl
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderId
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.model.SubscriptionName
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubscriptionMapperTest {

    private val subscription = Subscription(
        id = SubscriptionId("id-1"),
        providerId = ProviderId("spotify"),
        name = SubscriptionName.of("Spotify"),
        price = Money.ofUnits(34, 99),
        currency = Currency.PLN,
        billingPeriod = BillingPeriod.MONTHLY,
        managementUrl = ManagementUrl.ofOrNull("https://spotify.com/account"),
        nextBillingDate = LocalDate.of(2026, 9, 14),
        createdAt = Instant.parse("2026-08-20T09:00:00Z"),
        updatedAt = Instant.parse("2026-08-21T10:30:00Z"),
    )

    @Test
    fun `maps a subscription to primitive columns`() {
        val entity = SubscriptionMapper.toEntity(subscription)

        assertEquals("id-1", entity.id)
        assertEquals("spotify", entity.providerId)
        assertEquals(3499, entity.priceMinorUnits)
        assertEquals("PLN", entity.currencyCode)
        assertEquals("MONTHLY", entity.billingPeriod)
        assertEquals("https://spotify.com/account", entity.managementUrl)
        assertEquals(LocalDate.of(2026, 9, 14).toEpochDay(), entity.nextBillingDateEpochDay)
        assertEquals(subscription.createdAt.toEpochMilli(), entity.createdAtEpochMillis)
        assertEquals(subscription.updatedAt.toEpochMilli(), entity.updatedAtEpochMillis)
    }

    @Test
    fun `round trip keeps every value`() {
        val restored = SubscriptionMapper.toDomain(SubscriptionMapper.toEntity(subscription))

        assertEquals(subscription, restored)
    }

    @Test
    fun `optional columns map to null`() {
        val withoutOptionals = subscription.copy(managementUrl = null, nextBillingDate = null)

        val entity = SubscriptionMapper.toEntity(withoutOptionals)
        val restored = SubscriptionMapper.toDomain(entity)

        assertNull(entity.managementUrl)
        assertNull(entity.nextBillingDateEpochDay)
        assertEquals(withoutOptionals, restored)
    }
}
