package com.griff.keeper.infrastructure.backup.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of a backup payload.
 *
 * A parallel set of types rather than serialized domain objects, and deliberately so:
 *
 * - the domain models are value objects with invariants; deserializing straight into them would mean
 *   a malformed file could construct one, or crash trying;
 * - a rename or a refactor in the domain must never change the bytes of a file that has already been
 *   written to a user's Drive;
 * - every field here is a primitive, so nothing in the format can carry code. There is no polymorphic
 *   serialization, no class name in the JSON and no reflective instantiation - the parser can only
 *   produce these types, and only [BackupDtoMapper] turns them into records.
 *
 * Amounts travel as integer minor units and dates as epoch days: no `Double` ever touches a money
 * value, on the way out or the way back in.
 */
@Serializable
internal data class BackupPayloadDto(
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("exportedAtEpochMillis") val exportedAtEpochMillis: Long,
    @SerialName("appVersion") val appVersion: String,
    @SerialName("subscriptions") val subscriptions: List<SubscriptionDto> = emptyList(),
    @SerialName("obligations") val obligations: List<ObligationDto> = emptyList(),
    @SerialName("settings") val settings: SettingsDto,
)

@Serializable
internal data class SubscriptionDto(
    @SerialName("id") val id: String,
    @SerialName("providerId") val providerId: String,
    @SerialName("name") val name: String,
    /** `null` for catalog entries, which take their category from the provider catalog. */
    @SerialName("category") val category: String? = null,
    @SerialName("priceMinorUnits") val priceMinorUnits: Long,
    @SerialName("currency") val currency: String,
    @SerialName("billingPeriod") val billingPeriod: String,
    @SerialName("managementUrl") val managementUrl: String? = null,
    @SerialName("nextBillingDateEpochDay") val nextBillingDateEpochDay: Long? = null,
    @SerialName("remindersEnabled") val remindersEnabled: Boolean,
    @SerialName("createdAtEpochMillis") val createdAtEpochMillis: Long,
    @SerialName("updatedAtEpochMillis") val updatedAtEpochMillis: Long,
)

@Serializable
internal data class ObligationDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("amountMinorUnits") val amountMinorUnits: Long,
    @SerialName("currency") val currency: String,
    @SerialName("paymentStatus") val paymentStatus: String,
    @SerialName("paymentDateEpochDay") val paymentDateEpochDay: Long? = null,
    @SerialName("dueDateEpochDay") val dueDateEpochDay: Long? = null,
    @SerialName("validUntilEpochDay") val validUntilEpochDay: Long? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("remindersEnabled") val remindersEnabled: Boolean,
    @SerialName("createdAtEpochMillis") val createdAtEpochMillis: Long,
    @SerialName("updatedAtEpochMillis") val updatedAtEpochMillis: Long,
)

/**
 * The portable preferences.
 *
 * [reminderDefaults] is written for forward compatibility: the schedules are constants in this
 * build, so an import cannot change them, but a later version that lets the user edit them will be
 * able to read files written today without a schema bump.
 */
@Serializable
internal data class SettingsDto(
    @SerialName("globalRemindersEnabled") val globalRemindersEnabled: Boolean,
    @SerialName("reminderDefaults") val reminderDefaults: ReminderDefaultsDto,
)

@Serializable
internal data class ReminderDefaultsDto(
    @SerialName("insuranceDaysBefore") val insuranceDaysBefore: List<Int>,
    @SerialName("paymentDaysBefore") val paymentDaysBefore: List<Int>,
    @SerialName("subscriptionDaysBefore") val subscriptionDaysBefore: List<Int>,
)
