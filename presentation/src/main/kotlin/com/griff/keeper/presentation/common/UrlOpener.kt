package com.griff.keeper.presentation.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * Returns a function that opens a web address in an external browser.
 *
 * The function reports failures instead of throwing, so a device without a browser (or a malformed
 * address that survived validation) only results in a snackbar.
 */
@Composable
fun rememberUrlOpener(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            val uri: Uri? = runCatching { url.toUri() }.getOrNull()
            val scheme = uri?.scheme?.lowercase()
            if (uri == null || scheme !in ALLOWED_SCHEMES) {
                false
            } else {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                }
            }
        }
    }
}

private val ALLOWED_SCHEMES = setOf("http", "https")
