package com.griff.keeper.domain.backup

import java.io.InputStream
import java.io.OutputStream

/**
 * A place the app may write one backup to, chosen by the user.
 *
 * The abstraction exists so that use cases never see an `android.net.Uri`, a picker or a
 * `ContentResolver`: the platform layer resolves the user's choice into a stream, and everything
 * above it only knows that bytes can be written once.
 */
interface BackupSink {

    /** Opens the destination for writing. The caller closes it. */
    fun openOutputStream(): OutputStream
}

/**
 * A file the user pointed at, to be read as a candidate backup.
 *
 * [sizeBytes] is a hint from the platform and is `null` when it is unknown; it is used to refuse an
 * implausible file early, never to trust its contents.
 */
interface BackupSource {

    val displayName: String?

    val sizeBytes: Long?

    /** Opens the file for reading. The caller closes it. */
    fun openInputStream(): InputStream
}
