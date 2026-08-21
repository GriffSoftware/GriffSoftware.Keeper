package com.griff.keeper.presentation.datatransfer

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.griff.keeper.domain.backup.BackupSink
import com.griff.keeper.domain.backup.BackupSource
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The Android boundary of the backup feature.
 *
 * This is the only place that knows what an `android.net.Uri` is. The system document picker returns
 * one, and it is resolved here into the plain stream abstractions the use cases work with, so nothing
 * below the presentation layer ever sees a `Uri`, a `ContentResolver` or an `Intent`.
 *
 * A `content://` URI from the picker also carries its own access grant, which is why no storage
 * permission is involved: the app is given the one document the user selected and nothing else.
 */
internal fun Uri.asBackupSink(resolver: ContentResolver): BackupSink = object : BackupSink {

    override fun openOutputStream(): OutputStream =
        // "wt" truncates an existing file. Overwriting a previous backup must not leave the tail of
        // the old one behind - that would be a file with our magic bytes and trailing garbage.
        resolver.openOutputStream(this@asBackupSink, "wt")
            ?: throw IOException("Cannot open the selected document for writing")
}

internal fun Uri.asBackupSource(resolver: ContentResolver): BackupSource = object : BackupSource {

    override val displayName: String? = resolver.queryDisplayName(this@asBackupSource)

    override val sizeBytes: Long? = resolver.querySize(this@asBackupSource)

    override fun openInputStream(): InputStream =
        resolver.openInputStream(this@asBackupSource)
            ?: throw IOException("Cannot open the selected document for reading")
}

/**
 * The name the provider gives the document, used for the history entry.
 *
 * Only ever displayed. The name is never trusted to decide whether a file is a backup - the user is
 * free to rename it, and the format is recognized from its contents. Falls back to the last path
 * segment, which is what a provider that reports no metadata leaves us with.
 */
internal fun Uri.documentDisplayName(resolver: ContentResolver): String? =
    resolver.queryDisplayName(this)
        ?: lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf(String::isNotBlank)

private fun ContentResolver.queryDisplayName(uri: Uri): String? =
    queryColumn(uri, OpenableColumns.DISPLAY_NAME) { cursor, index ->
        cursor.takeIf { !it.isNull(index) }?.getString(index)
    }

private fun ContentResolver.querySize(uri: Uri): Long? =
    queryColumn(uri, OpenableColumns.SIZE) { cursor, index ->
        cursor.takeIf { !it.isNull(index) }?.getLong(index)
    }

/**
 * Reads one metadata column, or `null`.
 *
 * A content provider is another app: it may report nothing, report a null, or fail outright, and none
 * of that is a reason to stop an import. Both callers treat a missing value as "unknown", which the
 * layers above already handle.
 */
private inline fun <T> ContentResolver.queryColumn(
    uri: Uri,
    column: String,
    read: (Cursor, Int) -> T?,
): T? = runCatching {
    query(uri, arrayOf(column), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val index = cursor.getColumnIndex(column)
        if (index < 0) null else read(cursor, index)
    }
}.getOrNull()
