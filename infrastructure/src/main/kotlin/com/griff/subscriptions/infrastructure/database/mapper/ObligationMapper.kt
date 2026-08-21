package com.griff.subscriptions.infrastructure.database.mapper

import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.model.ObligationName
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.model.PaymentStatus
import com.griff.subscriptions.infrastructure.database.entity.ObligationEntity
import java.time.Instant
import java.time.LocalDate

/** Translates between the Room entity and the domain model. */
internal object ObligationMapper {

    fun toDomain(entity: ObligationEntity): Obligation = Obligation(
        id = ObligationId(entity.id),
        name = ObligationName.of(entity.name),
        category = ObligationCategory.valueOf(entity.category),
        amount = Money.ofMinorUnits(entity.amountMinorUnits),
        currency = Currency.fromCode(entity.currencyCode),
        payment = entity.paymentState(),
        dueDate = entity.dueDateEpochDay?.let(LocalDate::ofEpochDay),
        validUntil = entity.validUntilEpochDay?.let(LocalDate::ofEpochDay),
        notes = entity.notes,
        remindersEnabled = entity.remindersEnabled,
        createdAt = Instant.ofEpochMilli(entity.createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(entity.updatedAtEpochMillis),
    )

    fun toEntity(obligation: Obligation): ObligationEntity = ObligationEntity(
        id = obligation.id.value,
        name = obligation.name.value,
        category = obligation.category.name,
        amountMinorUnits = obligation.amount.minorUnits,
        currencyCode = obligation.currency.code,
        paymentStatus = obligation.payment.status.name,
        paymentDateEpochDay = obligation.payment.paymentDate?.toEpochDay(),
        dueDateEpochDay = obligation.dueDate?.toEpochDay(),
        validUntilEpochDay = obligation.validUntil?.toEpochDay(),
        notes = obligation.notes,
        remindersEnabled = obligation.remindersEnabled,
        createdAtEpochMillis = obligation.createdAt.toEpochMilli(),
        updatedAtEpochMillis = obligation.updatedAt.toEpochMilli(),
    )

    /**
     * Rebuilds the payment state from its two columns.
     *
     * A row claiming to be paid without a date can only come from outside this mapper; it is read
     * back as unpaid rather than crashing, so a single bad row never makes the list unopenable.
     */
    private fun ObligationEntity.paymentState(): PaymentState {
        val status = PaymentStatus.valueOf(paymentStatus)
        val date = paymentDateEpochDay?.let(LocalDate::ofEpochDay)
        return PaymentState.ofOrNull(status, date) ?: PaymentState.Unpaid
    }
}
