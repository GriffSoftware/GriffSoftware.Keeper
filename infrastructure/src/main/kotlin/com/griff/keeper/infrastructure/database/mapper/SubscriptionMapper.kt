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
import com.griff.keeper.infrastructure.database.entity.SubscriptionEntity
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
        remindersEnabled = entity.remindersEnabled,
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
        remindersEnabled = subscription.remindersEnabled,
        createdAtEpochMillis = subscription.createdAt.toEpochMilli(),
        updatedAtEpochMillis = subscription.updatedAt.toEpochMilli(),
    )
}
