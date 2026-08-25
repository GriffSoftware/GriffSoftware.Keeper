package com.griff.keeper.infrastructure.di

import com.griff.keeper.domain.currency.CurrencyConversionRepository
import com.griff.keeper.domain.repository.AppCurrencyRepository
import com.griff.keeper.infrastructure.repository.RoomCurrencyConversionRepository
import com.griff.keeper.infrastructure.settings.AppCurrencyDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the currency ports to their implementations.
 *
 * A module of its own for the same reason [BackupModule] is: the bindings here are the seams of one
 * feature - the global app currency and its conversion - and belong together rather than lost among
 * [InfrastructureModule]'s unrelated bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface CurrencyModule {

    @Binds
    @Singleton
    fun bindAppCurrencyRepository(impl: AppCurrencyDataStore): AppCurrencyRepository

    @Binds
    @Singleton
    fun bindCurrencyConversionRepository(
        impl: RoomCurrencyConversionRepository,
    ): CurrencyConversionRepository
}
