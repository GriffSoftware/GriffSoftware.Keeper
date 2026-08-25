package com.griff.keeper.infrastructure.database.mapper

import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ManagementUrl
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.SubscriptionId
import com.griff.keeper.domain.model.SubscriptionName
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
        categoryOverride = null,
        price = Money.ofUnits(34, 99),
        currency = Currency.PLN,
        billingPeriod = BillingPeriod.MONTHLY,
        managementUrl = ManagementUrl.ofOrNull("https://spotify.com/account"),
        nextBillingDate = LocalDate.of(2026, 9, 14),
        remindersEnabled = true,
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
        assertEquals(true, entity.remindersEnabled)
        assertEquals(subscription.createdAt.toEpochMilli(), entity.createdAtEpochMillis)
        assertEquals(subscription.updatedAt.toEpochMilli(), entity.updatedAtEpochMillis)
    }

    @Test
    fun `round trip keeps every value`() {
        val restored = SubscriptionMapper.toDomain(SubscriptionMapper.toEntity(subscription))

        assertEquals(subscription, restored)
    }

    @Test
    fun `a catalog entry stores no category of its own`() {
        val entity = SubscriptionMapper.toEntity(subscription)

        assertNull(entity.category)
        assertNull(SubscriptionMapper.toDomain(entity).categoryOverride)
    }

    @Test
    fun `a custom entry keeps the category the user picked`() {
        val custom = subscription.copy(
            providerId = ProviderId.OTHER,
            categoryOverride = ProviderCategory.HOSTING,
        )

        val entity = SubscriptionMapper.toEntity(custom)

        assertEquals("HOSTING", entity.category)
        assertEquals(custom, SubscriptionMapper.toDomain(entity))
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

    @Test
    fun `a EUR subscription round trips with its own currency code`() {
        val eurSubscription = subscription.copy(currency = Currency.EUR)

        val entity = SubscriptionMapper.toEntity(eurSubscription)

        assertEquals("EUR", entity.currencyCode)
        assertEquals(eurSubscription, SubscriptionMapper.toDomain(entity))
    }
}
