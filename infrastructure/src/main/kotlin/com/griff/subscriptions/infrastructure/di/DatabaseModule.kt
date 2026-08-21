package com.griff.subscriptions.infrastructure.di

import android.content.Context
import androidx.room.Room
import com.griff.subscriptions.infrastructure.database.DatabaseMigrations
import com.griff.subscriptions.infrastructure.database.GriffDatabase
import com.griff.subscriptions.infrastructure.database.dao.ObligationDao
import com.griff.subscriptions.infrastructure.database.dao.ReminderEventDao
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
    fun provideDatabase(@ApplicationContext context: Context): GriffDatabase =
        Room.databaseBuilder(
            context = context,
            klass = GriffDatabase::class.java,
            name = GriffDatabase.NAME,
        )
            .addMigrations(*DatabaseMigrations)
            .build()

    @Provides
    fun provideSubscriptionDao(database: GriffDatabase): SubscriptionDao =
        database.subscriptionDao()

    @Provides
    fun provideObligationDao(database: GriffDatabase): ObligationDao = database.obligationDao()

    @Provides
    fun provideReminderEventDao(database: GriffDatabase): ReminderEventDao =
        database.reminderEventDao()
}
