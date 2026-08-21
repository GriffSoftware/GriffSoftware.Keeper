package com.griff.keeper.infrastructure.di

import com.griff.keeper.domain.backup.BackupCodec
import com.griff.keeper.domain.backup.BackupFileReader
import com.griff.keeper.domain.backup.BackupFileSharing
import com.griff.keeper.domain.backup.BackupFileWriter
import com.griff.keeper.domain.backup.BackupImportRepository
import com.griff.keeper.domain.backup.BackupOperationRepository
import com.griff.keeper.domain.backup.NetworkAvailability
import com.griff.keeper.domain.backup.PortableSettingsRepository
import com.griff.keeper.domain.id.BackupOperationIdGenerator
import com.griff.keeper.infrastructure.backup.AndroidNetworkAvailability
import com.griff.keeper.infrastructure.backup.EncryptedBackupCodec
import com.griff.keeper.infrastructure.backup.FileProviderBackupSharing
import com.griff.keeper.infrastructure.backup.StreamingBackupFileReader
import com.griff.keeper.infrastructure.backup.StreamingBackupFileWriter
import com.griff.keeper.infrastructure.id.UuidBackupOperationIdGenerator
import com.griff.keeper.infrastructure.repository.RoomBackupImportRepository
import com.griff.keeper.infrastructure.repository.RoomBackupOperationRepository
import com.griff.keeper.infrastructure.settings.PortableSettingsDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the backup ports to their implementations.
 *
 * A module of its own rather than more entries in [InfrastructureModule]: the bindings here are the
 * seams of one feature, and the split makes it obvious that no single class owns crypto, files, the
 * database, sharing and connectivity at once.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface BackupModule {

    @Binds
    @Singleton
    fun bindBackupCodec(impl: EncryptedBackupCodec): BackupCodec

    @Binds
    @Singleton
    fun bindBackupFileReader(impl: StreamingBackupFileReader): BackupFileReader

    @Binds
    @Singleton
    fun bindBackupFileWriter(impl: StreamingBackupFileWriter): BackupFileWriter

    @Binds
    @Singleton
    fun bindBackupFileSharing(impl: FileProviderBackupSharing): BackupFileSharing

    @Binds
    @Singleton
    fun bindBackupOperationRepository(
        impl: RoomBackupOperationRepository,
    ): BackupOperationRepository

    @Binds
    @Singleton
    fun bindBackupImportRepository(impl: RoomBackupImportRepository): BackupImportRepository

    @Binds
    @Singleton
    fun bindPortableSettingsRepository(
        impl: PortableSettingsDataStore,
    ): PortableSettingsRepository

    @Binds
    @Singleton
    fun bindNetworkAvailability(impl: AndroidNetworkAvailability): NetworkAvailability

    @Binds
    @Singleton
    fun bindBackupOperationIdGenerator(
        impl: UuidBackupOperationIdGenerator,
    ): BackupOperationIdGenerator
}
