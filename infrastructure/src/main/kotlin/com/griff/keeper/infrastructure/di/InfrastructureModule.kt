package com.griff.keeper.infrastructure.di

import com.griff.keeper.domain.id.ObligationIdGenerator
import com.griff.keeper.domain.id.SubscriptionIdGenerator
import com.griff.keeper.domain.reminder.NotificationAvailability
import com.griff.keeper.domain.reminder.ReminderEventStore
import com.griff.keeper.domain.reminder.ReminderPublisher
import com.griff.keeper.domain.reminder.ReminderScheduler
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.repository.ReminderSettingsRepository
import com.griff.keeper.domain.repository.ProviderCatalog
import com.griff.keeper.domain.repository.SubscriptionRepository
import com.griff.keeper.domain.time.ClockProvider
import com.griff.keeper.infrastructure.catalog.StaticProviderCatalog
import com.griff.keeper.infrastructure.id.UuidObligationIdGenerator
import com.griff.keeper.infrastructure.id.UuidSubscriptionIdGenerator
import com.griff.keeper.infrastructure.reminder.AndroidNotificationAvailability
import com.griff.keeper.infrastructure.reminder.AndroidReminderPublisher
import com.griff.keeper.infrastructure.reminder.WorkManagerReminderScheduler
import com.griff.keeper.infrastructure.repository.RoomObligationRepository
import com.griff.keeper.infrastructure.repository.RoomReminderEventStore
import com.griff.keeper.infrastructure.settings.ReminderSettingsDataStore
import com.griff.keeper.infrastructure.repository.RoomSubscriptionRepository
import com.griff.keeper.infrastructure.time.SystemClockProvider
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
