package com.griff.keeper.domain.testing

import com.griff.keeper.domain.id.ObligationIdGenerator
import com.griff.keeper.domain.id.SubscriptionIdGenerator
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ManagementUrl
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.ObligationId
import com.griff.keeper.domain.model.ObligationName
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.domain.model.Provider
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.SubscriptionId
import com.griff.keeper.domain.model.SubscriptionName
import com.griff.keeper.domain.reminder.NotificationAvailability
import com.griff.keeper.domain.reminder.ReminderEventStore
import com.griff.keeper.domain.reminder.ReminderNotification
import com.griff.keeper.domain.reminder.ReminderPublisher
import com.griff.keeper.domain.reminder.ReminderScheduler
import com.griff.keeper.domain.reminder.ReminderSettings
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.repository.ProviderCatalog
import com.griff.keeper.domain.repository.ReminderSettingsRepository
import com.griff.keeper.domain.repository.SubscriptionRepository
import com.griff.keeper.domain.time.ClockProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Test doubles shared by the tests of every layer.
 *
 * They live in the domain module because they implement domain ports; production code never sees
 * them (`testFixtures` is only on the test classpath).
 */
class FakeSubscriptionRepository(
    initial: List<Subscription> = emptyList(),
) : SubscriptionRepository {

    private val state = MutableStateFlow(initial.sortedBy { it.name.value.lowercase() })

    var failOnWrite: Boolean = false

    val stored: List<Subscription> get() = state.value

    override fun observeAll(): Flow<List<Subscription>> = state

    override fun observeById(id: SubscriptionId): Flow<Subscription?> =
        state.map { subscriptions -> subscriptions.firstOrNull { it.id == id } }

    override suspend fun findById(id: SubscriptionId): Subscription? =
        state.value.firstOrNull { it.id == id }

    override suspend fun add(subscription: Subscription) {
        failIfRequested()
        state.value = (state.value + subscription).sortedBy { it.name.value.lowercase() }
    }

    override suspend fun update(subscription: Subscription) {
        failIfRequested()
        state.value = state.value.map { if (it.id == subscription.id) subscription else it }
    }

    override suspend fun delete(id: SubscriptionId) {
        failIfRequested()
        state.value = state.value.filterNot { it.id == id }
    }

    private fun failIfRequested() {
        if (failOnWrite) error("Simulated storage failure")
    }
}

/** In-memory [ObligationRepository], ordered the way the Room implementation orders rows. */
class FakeObligationRepository(
    initial: List<Obligation> = emptyList(),
) : ObligationRepository {

    private val state = MutableStateFlow(initial.sortedBy { it.name.value.lowercase() })

    var failOnWrite: Boolean = false

    val stored: List<Obligation> get() = state.value

    override fun observeAll(): Flow<List<Obligation>> = state

    override fun observeById(id: ObligationId): Flow<Obligation?> =
        state.map { obligations -> obligations.firstOrNull { it.id == id } }

    override suspend fun findById(id: ObligationId): Obligation? =
        state.value.firstOrNull { it.id == id }

    override suspend fun add(obligation: Obligation) {
        failIfRequested()
        state.value = (state.value + obligation).sortedBy { it.name.value.lowercase() }
    }

    override suspend fun update(obligation: Obligation) {
        failIfRequested()
        state.value = state.value.map { if (it.id == obligation.id) obligation else it }
    }

    override suspend fun delete(id: ObligationId) {
        failIfRequested()
        state.value = state.value.filterNot { it.id == id }
    }

    private fun failIfRequested() {
        if (failOnWrite) error("Simulated storage failure")
    }
}

/** Clock frozen at a known instant so date dependent assertions are stable. */
class FixedClockProvider(
    private var instant: Instant = Instant.parse("2026-08-20T09:00:00Z"),
    private val zone: ZoneId = ZoneOffset.UTC,
) : ClockProvider {

    override fun zone(): ZoneId = zone

    override fun now(): Instant = instant

    fun advanceTo(value: Instant) {
        instant = value
    }
}

/** Generates predictable ids: `id-1`, `id-2`, ... */
class SequentialIdGenerator : SubscriptionIdGenerator {
    private var counter = 0

    override fun next(): SubscriptionId {
        counter++
        return SubscriptionId("id-$counter")
    }
}

/** Generates predictable obligation ids: `obligation-1`, `obligation-2`, ... */
class SequentialObligationIdGenerator : ObligationIdGenerator {
    private var counter = 0

    override fun next(): ObligationId {
        counter++
        return ObligationId("obligation-$counter")
    }
}

/** Small catalog with one entry per tested category plus the mandatory "Other" entry. */
class FakeProviderCatalog(
    private val providers: List<Provider> = DefaultProviders,
) : ProviderCatalog {

    override fun all(): List<Provider> = providers

    override fun findById(id: ProviderId): Provider? = providers.firstOrNull { it.id == id }

    override fun other(): Provider = providers.first { it.isOther }

    companion object {
        val DefaultProviders: List<Provider> = listOf(
            catalogProvider("spotify", "Spotify", ProviderCategory.MUSIC),
            catalogProvider("netflix", "Netflix", ProviderCategory.VIDEO),
            catalogProvider("seohost", "SeoHost.pl", ProviderCategory.HOSTING),
            Provider(
                id = ProviderId.OTHER,
                displayName = "Other",
                category = ProviderCategory.OTHER,
                logoKey = ProviderId.OTHER.value,
                defaultManagementUrl = null,
            ),
        )

        private fun catalogProvider(
            id: String,
            name: String,
            category: ProviderCategory,
        ) = Provider(
            id = ProviderId(id),
            displayName = name,
            category = category,
            logoKey = id,
            defaultManagementUrl = ManagementUrl.ofOrNull("https://$id.example.com"),
        )
    }
}

fun testSubscription(
    id: String = "id-1",
    providerId: String = "spotify",
    name: String = "Spotify",
    categoryOverride: ProviderCategory? = null,
    priceMinorUnits: Long = 3499,
    billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    nextBillingDate: LocalDate? = null,
    managementUrl: String? = null,
    remindersEnabled: Boolean = true,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): Subscription = Subscription(
    id = SubscriptionId(id),
    providerId = ProviderId(providerId),
    name = SubscriptionName.of(name),
    categoryOverride = categoryOverride,
    price = Money.ofMinorUnits(priceMinorUnits),
    currency = Currency.PLN,
    billingPeriod = billingPeriod,
    managementUrl = managementUrl?.let(ManagementUrl::ofOrNull),
    nextBillingDate = nextBillingDate,
    remindersEnabled = remindersEnabled,
    createdAt = createdAt,
    updatedAt = createdAt,
)

fun testObligation(
    id: String = "obligation-1",
    name: String = "OC Ford",
    category: ObligationCategory = ObligationCategory.VEHICLE_INSURANCE,
    amountMinorUnits: Long = 124_000,
    payment: PaymentState = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
    dueDate: LocalDate? = null,
    validUntil: LocalDate? = LocalDate.of(2027, 3, 11),
    notes: String? = null,
    remindersEnabled: Boolean = true,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): Obligation = Obligation(
    id = ObligationId(id),
    name = ObligationName.of(name),
    category = category,
    amount = Money.ofMinorUnits(amountMinorUnits),
    currency = Currency.PLN,
    payment = payment,
    dueDate = dueDate,
    validUntil = validUntil,
    notes = notes,
    remindersEnabled = remindersEnabled,
    createdAt = createdAt,
    updatedAt = createdAt,
)

/** In-memory [ReminderSettingsRepository]; the global switch is the only stored preference. */
class FakeReminderSettingsRepository(
    initial: ReminderSettings = ReminderSettings.Default,
) : ReminderSettingsRepository {

    private val state = MutableStateFlow(initial)

    override fun observe(): Flow<ReminderSettings> = state

    override suspend fun current(): ReminderSettings = state.value

    override suspend fun setGlobalEnabled(enabled: Boolean) {
        state.value = state.value.copy(globalEnabled = enabled)
    }
}

/** In-memory deduplication ledger, with the same "record once" semantics as the real table. */
class FakeReminderEventStore(initial: Set<String> = emptySet()) : ReminderEventStore {

    private val state = MutableStateFlow(initial)

    val keys: Set<String> get() = state.value

    override fun observeDeliveredKeys(): Flow<Set<String>> = state

    override suspend fun deliveredKeys(): Set<String> = state.value

    override suspend fun markDelivered(key: String, sentAt: Instant) {
        sentTimes[key] = sentAt
        state.value = state.value + key
    }

    override suspend fun deleteSentBefore(threshold: Instant) {
        val expired = sentTimes.filterValues { it < threshold }.keys
        expired.forEach(sentTimes::remove)
        state.value = state.value - expired
    }

    private val sentTimes = mutableMapOf<String, Instant>()
}

/** Records what would have been shown, so tests can assert on reminders without Android. */
class RecordingReminderPublisher : ReminderPublisher {

    private val _published = mutableListOf<ReminderNotification>()
    val published: List<ReminderNotification> get() = _published

    override suspend fun publish(notification: ReminderNotification) {
        _published += notification
    }
}

/** Stands in for the Android notification permission. */
class FakeNotificationAvailability(var enabled: Boolean = true) : NotificationAvailability {
    override fun areNotificationsEnabled(): Boolean = enabled
}

/** Counts scheduling requests, which have to be idempotent. */
class RecordingReminderScheduler : ReminderScheduler {
    var scheduleCount: Int = 0
        private set

    override fun ensureScheduled() {
        scheduleCount++
    }
}
