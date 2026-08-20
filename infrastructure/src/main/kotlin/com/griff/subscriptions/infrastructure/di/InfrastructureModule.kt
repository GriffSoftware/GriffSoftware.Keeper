package com.griff.subscriptions.infrastructure.di

import com.griff.subscriptions.domain.id.SubscriptionIdGenerator
import com.griff.subscriptions.domain.repository.ProviderCatalog
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.infrastructure.catalog.StaticProviderCatalog
import com.griff.subscriptions.infrastructure.id.UuidSubscriptionIdGenerator
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
    fun bindProviderCatalog(impl: StaticProviderCatalog): ProviderCatalog

    @Binds
    @Singleton
    fun bindClockProvider(impl: SystemClockProvider): ClockProvider

    @Binds
    @Singleton
    fun bindSubscriptionIdGenerator(impl: UuidSubscriptionIdGenerator): SubscriptionIdGenerator
}
