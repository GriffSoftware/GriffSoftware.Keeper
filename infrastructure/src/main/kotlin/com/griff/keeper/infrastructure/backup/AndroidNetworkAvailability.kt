package com.griff.keeper.infrastructure.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.griff.keeper.domain.backup.NetworkAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the current connectivity through [ConnectivityManager] and [NetworkCapabilities].
 *
 * [NetworkCapabilities.NET_CAPABILITY_VALIDATED] rather than "a network exists": a captive portal or
 * a Wi-Fi with no route out would otherwise be reported as being online, which is precisely the case
 * the warning is meant to catch.
 *
 * Needs `ACCESS_NETWORK_STATE`, which is a normal permission - granted at install, no runtime dialog.
 * The app does not gain `INTERNET`: it never opens a connection, and the mail is sent by whichever
 * client the user picks.
 */
@Singleton
class AndroidNetworkAvailability @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NetworkAvailability {

    override fun isOnline(): Boolean {
        val manager = context.getSystemService<ConnectivityManager>() ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
