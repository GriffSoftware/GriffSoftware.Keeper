package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.NetworkAvailability
import javax.inject.Inject

/**
 * Whether the device has a verified internet connection right now.
 *
 * Used for a warning, never for a block. The mail leaves through another app, which is free to queue
 * it until connectivity returns, so the app says what it knows and lets the user decide.
 */
class IsNetworkAvailableUseCase @Inject constructor(
    private val availability: NetworkAvailability,
) {
    operator fun invoke(): Boolean = availability.isOnline()
}
