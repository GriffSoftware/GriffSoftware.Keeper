package com.griff.keeper.app.di

import com.griff.keeper.BuildConfig
import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.application.appinfo.AppVersionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Supplies build information to the layers above.
 *
 * `BuildConfig` only exists in the app module, so this is the single place that reads it; the
 * drawer receives the real values through [AppVersionProvider].
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppVersionModule {

    @Provides
    @Singleton
    fun provideAppVersionProvider(): AppVersionProvider = object : AppVersionProvider {
        override fun version(): AppVersion = AppVersion(
            name = BuildConfig.VERSION_NAME,
            code = BuildConfig.VERSION_CODE.toLong(),
        )
    }
}
