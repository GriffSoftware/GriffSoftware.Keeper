package com.griff.subscriptions.infrastructure.di

import com.griff.subscriptions.domain.id.ObligationIdGenerator
import com.griff.subscriptions.domain.id.SubscriptionIdGenerator
import com.griff.subscriptions.domain.reminder.NotificationAvailability
import com.griff.subscriptions.domain.reminder.ReminderEventStore
import com.griff.subscriptions.domain.reminder.ReminderPublisher
import com.griff.subscriptions.domain.reminder.ReminderScheduler
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.repository.ReminderSettingsRepository
import com.griff.subscriptions.domain.repository.ProviderCatalog
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.infrastructure.catalog.StaticProviderCatalog
import com.griff.subscriptions.infrastructure.id.UuidObligationIdGenerator
import com.griff.subscriptions.infrastructure.id.UuidSubscriptionIdGenerator
import com.griff.subscriptions.infrastructure.reminder.AndroidNotificationAvailability
import com.griff.subscriptions.infrastructure.reminder.AndroidReminderPublisher
import com.griff.subscriptions.infrastructure.reminder.WorkManagerReminderScheduler
import com.griff.subscriptions.infrastructure.repository.RoomObligationRepository
import com.griff.subscriptions.infrastructure.repository.RoomReminderEventStore
import com.griff.subscriptions.infrastructure.settings.ReminderSettingsDataStore
import com.griff.subscriptions.infrastructure.repository.RoomSubscriptionRepository
import com.griff.subscriptions.infrastructure.time.SystemClockProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the domain ports to their infrastructure implementations. */
@Module
@InstallIn(SingletonComponent::class)
internal interface InfrastructureModule {

    @Binds
    @Singleton
    fun bindSubscriptionRepository(impl: RoomSubscriptionRepository): SubscriptionRepository

    @Binds
    @Singleton
    fun bindObligationRepository(impl: RoomObligationRepository): ObligationRepository

    @Binds
    @Singleton
    fun bindReminderSettingsRepository(
        impl: ReminderSettingsDataStore,
    ): ReminderSettingsRepository

    @Binds
    @Singleton
    fun bindReminderEventStore(impl: RoomReminderEventStore): ReminderEventStore

    @Binds
    @Singleton
    fun bindNotificationAvailability(
        impl: AndroidNotificationAvailability,
    ): NotificationAvailability

    @Binds
    @Singleton
    fun bindReminderPublisher(impl: AndroidReminderPublisher): ReminderPublisher

    @Binds
    @Singleton
    fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    @Binds
    @Singleton
    fun bindProviderCatalog(impl: StaticProviderCatalog): ProviderCatalog

    @Binds
    @Singleton
    fun bindClockProvider(impl: SystemClockProvider): ClockProvider

    @Binds
    @Singleton
    fun bindSubscriptionIdGenerator(impl: UuidSubscriptionIdGenerator): SubscriptionIdGenerator

    @Binds
    @Singleton
    fun bindObligationIdGenerator(impl: UuidObligationIdGenerator): ObligationIdGenerator
}
