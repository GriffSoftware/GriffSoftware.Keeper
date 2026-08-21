package com.griff.keeper.infrastructure.backup

import android.content.Context
import androidx.core.content.FileProvider
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupFileSharing
import com.griff.keeper.domain.backup.SharedBackupFile
import com.griff.keeper.infrastructure.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Exposes a finished backup to another app through a `content://` URI.
 *
 * `FileProvider` rather than a `file://` URI, which modern Android rejects outright and which would
 * hand out a path into the app's private storage. The provider grants read access to exactly one
 * file, only to the app the user picks in the chooser, and only for as long as the intent lives.
 *
 * The staging directory holds at most one file. Backups are encrypted, but they are still the user's
 * data, and leaving a growing pile of them in a cache nobody looks at is not a defensible default -
 * so every new share clears the previous one, and the caller clears it again when the chooser is
 * done.
 */
@Singleton
class FileProviderBackupSharing @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BackupFileSharing {

    override suspend fun stage(fileName: String, bytes: ByteArray): SharedBackupFile =
        withContext(dispatcher) {
            try {
                val directory = File(context.cacheDir, DIRECTORY_NAME).apply { mkdirs() }
                // The name is built by the app, never taken from a file the user picked, so it
                // cannot carry separators; sanitizing anyway keeps that a property of this method
                // rather than of its callers.
                val file = File(directory, fileName.sanitized())
                file.writeBytes(bytes)

                SharedBackupFile(
                    uri = FileProvider
                        .getUriForFile(context, "${context.packageName}.$AUTHORITY_SUFFIX", file)
                        .toString(),
                    fileName = file.name,
                )
            } catch (error: IOException) {
                throw BackupFailureException(error.storageErrorType(), error)
            } catch (error: IllegalArgumentException) {
                // FileProvider refuses paths outside its configured roots.
                throw BackupFailureException(BackupErrorType.IO_ERROR, error)
            }
        }

    override suspend fun clear() {
        withContext(dispatcher) {
            runCatching { File(context.cacheDir, DIRECTORY_NAME).deleteRecursively() }
        }
    }

    /** Keeps the name a name: no separators, no traversal, nothing surprising in a directory. */
    private fun String.sanitized(): String =
        substringAfterLast('/').substringAfterLast('\\').ifBlank { FALLBACK_FILE_NAME }

    private companion object {
        const val DIRECTORY_NAME = "backups"

        /** Matches the authority declared for the provider in the module's manifest. */
        const val AUTHORITY_SUFFIX = "backupfiles"

        const val FALLBACK_FILE_NAME = "griff-backup.griffbackup"
    }
}
