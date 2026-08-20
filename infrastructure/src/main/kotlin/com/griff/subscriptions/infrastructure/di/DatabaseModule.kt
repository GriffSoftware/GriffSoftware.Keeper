package com.griff.subscriptions.infrastructure.di

import android.content.Context
import androidx.room.Room
import com.griff.subscriptions.infrastructure.database.DatabaseMigrations
import com.griff.subscriptions.infrastructure.database.SubscriptionDatabase
import com.griff.subscriptions.infrastructure.database.dao.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SubscriptionDatabase =
        Room.databaseBuilder(
            context = context,
            klass = SubscriptionDatabase::class.java,
            name = SubscriptionDatabase.NAME,
        )
            .addMigrations(*DatabaseMigrations)
            .build()

    @Provides
    fun provideSubscriptionDao(database: SubscriptionDatabase): SubscriptionDao =
        database.subscriptionDao()
}
