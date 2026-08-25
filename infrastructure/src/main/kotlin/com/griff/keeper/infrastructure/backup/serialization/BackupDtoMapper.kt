package com.griff.keeper.infrastructure.backup.serialization

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ManagementUrl
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.ObligationId
import com.griff.keeper.domain.model.ObligationName
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.domain.model.PaymentStatus
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.SubscriptionId
import com.griff.keeper.domain.model.SubscriptionName
import com.griff.keeper.domain.reminder.ReminderDefaults
import com.griff.keeper.domain.reminder.ReminderSchedule
import java.time.Instant
import java.time.LocalDate

/**
 * Translates between the wire shape and the domain models.
 *
 * The direction that matters is inwards. Everything coming from a file is untrusted, so every value
 * is turned into a domain type through that type's own constructor - the same one a typed-in record
 * goes through - and anything the constructor refuses fails the whole import with
 * [BackupErrorType.VALIDATION_ERROR]. No value is coerced, clamped or silently defaulted: a record
 * the app cannot represent is a reason to refuse the file, not to invent a substitute for it.
 *
 * An unknown enum name is treated the same way. Mapping it to a fallback would look forgiving and
 * would quietly change what the user's record says; refusing is honest, and a genuinely new value
 * arriving from a newer app is already caught earlier by the schema version check.
 */
internal object BackupDtoMapper {

    fun toDto(payload: BackupPayload): BackupPayloadDto = BackupPayloadDto(
        schemaVersion = payload.schemaVersion,
        exportedAtEpochMillis = payload.exportedAt.toEpochMilli(),
        appVersion = payload.appVersion,
        subscriptions = payload.subscriptions.map(::toDto),
        obligations = payload.obligations.map(::toDto),
        settings = toDto(payload.settings),
    )

    fun toDomain(dto: BackupPayloadDto): BackupPayload = validated {
        BackupPayload(
            schemaVersion = dto.schemaVersion,
            exportedAt = Instant.ofEpochMilli(dto.exportedAtEpochMillis),
            appVersion = dto.appVersion,
            subscriptions = dto.subscriptions.map(::toDomain),
            obligations = dto.obligations.map(::toDomain),
            settings = toDomain(dto.settings),
        )
    }

    private fun toDto(subscription: Subscription) = SubscriptionDto(
        id = subscription.id.value,
        providerId = subscription.providerId.value,
        name = subscription.name.value,
        category = subscription.categoryOverride?.name,
        priceMinorUnits = subscription.price.minorUnits,
        currency = subscription.currency.code,
        billingPeriod = subscription.billingPeriod.name,
        managementUrl = subscription.managementUrl?.value,
        nextBillingDateEpochDay = subscription.nextBillingDate?.toEpochDay(),
        remindersEnabled = subscription.remindersEnabled,
        createdAtEpochMillis = subscription.createdAt.toEpochMilli(),
        updatedAtEpochMillis = subscription.updatedAt.toEpochMilli(),
    )

    private fun toDomain(dto: SubscriptionDto) = Subscription(
        id = SubscriptionId(dto.id),
        providerId = ProviderId(dto.providerId),
        name = SubscriptionName.of(dto.name),
        categoryOverride = dto.category?.let { enumOrFail<ProviderCategory>(it) },
        price = Money.ofMinorUnits(dto.priceMinorUnits),
        currency = Currency.fromCode(dto.currency),
        billingPeriod = enumOrFail<BillingPeriod>(dto.billingPeriod),
        // A stored address that no longer parses is a refusal, not a silent `null`: dropping it
        // would hand the user back a record that is quietly missing something they had entered.
        managementUrl = dto.managementUrl?.let { raw ->
            ManagementUrl.ofOrNull(raw) ?: fail()
        },
        nextBillingDate = dto.nextBillingDateEpochDay?.toLocalDate(),
        remindersEnabled = dto.remindersEnabled,
        createdAt = Instant.ofEpochMilli(dto.createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(dto.updatedAtEpochMillis),
    )

    private fun toDto(obligation: Obligation) = ObligationDto(
        id = obligation.id.value,
        name = obligation.name.value,
        category = obligation.category.name,
        amountMinorUnits = obligation.amount.minorUnits,
        currency = obligation.currency.code,
        paymentStatus = obligation.payment.status.name,
        paymentDateEpochDay = obligation.payment.paymentDate?.toEpochDay(),
        dueDateEpochDay = obligation.dueDate?.toEpochDay(),
        validUntilEpochDay = obligation.validUntil?.toEpochDay(),
        notes = obligation.notes,
        remindersEnabled = obligation.remindersEnabled,
        createdAtEpochMillis = obligation.createdAt.toEpochMilli(),
        updatedAtEpochMillis = obligation.updatedAt.toEpochMilli(),
    )

    private fun toDomain(dto: ObligationDto) = Obligation(
        id = ObligationId(dto.id),
        name = ObligationName.of(dto.name),
        category = enumOrFail<ObligationCategory>(dto.category),
        amount = Money.ofMinorUnits(dto.amountMinorUnits),
        currency = Currency.fromCode(dto.currency),
        // "Paid" without a date could never be attributed to a year, so the pair has to agree.
        payment = PaymentState.ofOrNull(
            status = enumOrFail<PaymentStatus>(dto.paymentStatus),
            paymentDate = dto.paymentDateEpochDay?.toLocalDate(),
        ) ?: fail(),
        dueDate = dto.dueDateEpochDay?.toLocalDate(),
        validUntil = dto.validUntilEpochDay?.toLocalDate(),
        notes = dto.notes,
        remindersEnabled = dto.remindersEnabled,
        createdAt = Instant.ofEpochMilli(dto.createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(dto.updatedAtEpochMillis),
    )

    private fun toDto(settings: PortableSettings) = SettingsDto(
        globalRemindersEnabled = settings.globalRemindersEnabled,
        reminderDefaults = ReminderDefaultsDto(
            insuranceDaysBefore = settings.reminderDefaults.insurance.daysBefore,
            paymentDaysBefore = settings.reminderDefaults.payment.daysBefore,
            subscriptionDaysBefore = settings.reminderDefaults.subscription.daysBefore,
        ),
        appCurrency = settings.appCurrency.code,
    )

    private fun toDomain(dto: SettingsDto) = PortableSettings(
        globalRemindersEnabled = dto.globalRemindersEnabled,
        reminderDefaults = ReminderDefaults(
            insurance = schedule(dto.reminderDefaults.insuranceDaysBefore),
            payment = schedule(dto.reminderDefaults.paymentDaysBefore),
            subscription = schedule(dto.reminderDefaults.subscriptionDaysBefore),
        ),
        // Absent exactly for a backup written before this feature existed; such a file could only
        // ever have held PLN, never the currency the importing device happens to be using now.
        appCurrency = dto.appCurrency?.let { Currency.fromCodeOrNull(it) ?: fail() } ?: Currency.PLN,
    )

    private fun schedule(daysBefore: List<Int>): ReminderSchedule =
        if (daysBefore.any { it < 0 }) fail() else ReminderSchedule.of(daysBefore)

    private fun Long.toLocalDate(): LocalDate =
        runCatching { LocalDate.ofEpochDay(this) }.getOrElse { fail() }

    private inline fun <reified T : Enum<T>> enumOrFail(name: String): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fail()

    /**
     * Turns any refusal from a domain constructor into one category.
     *
     * The constructors signal with `require`/`error`, which is the right thing for a programming
     * error and the wrong thing to show a user, so the boundary converts them once instead of every
     * mapper doing its own defensive checks.
     */
    private inline fun <T> validated(block: () -> T): T = try {
        block()
    } catch (failure: BackupFailureException) {
        throw failure
    } catch (error: RuntimeException) {
        throw BackupFailureException(BackupErrorType.VALIDATION_ERROR, error)
    }

    private fun fail(): Nothing = throw BackupFailureException(BackupErrorType.VALIDATION_ERROR)
}
