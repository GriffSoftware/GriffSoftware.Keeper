package com.griff.subscriptions.infrastructure.database.mapper

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.ManagementUrl
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.ProviderId
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.model.SubscriptionName
import com.griff.subscriptions.infrastructure.database.entity.SubscriptionEntity
import java.time.Instant
import java.time.LocalDate

/** Translates between the Room entity and the domain model. */
internal object SubscriptionMapper {

    fun toDomain(entity: SubscriptionEntity): Subscription = Subscription(
        id = SubscriptionId(entity.id),
        providerId = ProviderId(entity.providerId),
        name = SubscriptionName.of(entity.name),
        categoryOverride = entity.category?.let { ProviderCategory.valueOf(it) },
        price = Money.ofMinorUnits(entity.priceMinorUnits),
        currency = Currency.fromCode(entity.currencyCode),
        billingPeriod = BillingPeriod.valueOf(entity.billingPeriod),
        managementUrl = entity.managementUrl?.let(ManagementUrl::ofOrNull),
        nextBillingDate = entity.nextBillingDateEpochDay?.let(LocalDate::ofEpochDay),
        createdAt = Instant.ofEpochMilli(entity.createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(entity.updatedAtEpochMillis),
    )

    fun toEntity(subscription: Subscription): SubscriptionEntity = SubscriptionEntity(
        id = subscription.id.value,
        providerId = subscription.providerId.value,
        name = subscription.name.value,
        category = subscription.categoryOverride?.name,
        priceMinorUnits = subscription.price.minorUnits,
        currencyCode = subscription.currency.code,
        billingPeriod = subscription.billingPeriod.name,
        managementUrl = subscription.managementUrl?.value,
        nextBillingDateEpochDay = subscription.nextBillingDate?.toEpochDay(),
        createdAtEpochMillis = subscription.createdAt.toEpochMilli(),
        updatedAtEpochMillis = subscription.updatedAt.toEpochMilli(),
    )
}
