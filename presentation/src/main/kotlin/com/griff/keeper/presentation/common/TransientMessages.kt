package com.griff.keeper.presentation.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Carries one transient message across a navigation boundary.
 *
 * Some feedback belongs to a screen the user is no longer on. A subscription is deleted on its details
 * screen, which then disappears; a record is saved on a form that closes itself. The confirmation has
 * to appear on whatever the user lands on, so it cannot live in the ViewModel of the screen that
 * caused it.
 *
 * Held by the composition that owns the navigation graph and handed to the screens that can show it.
 * Deliberately *not* a singleton event bus: its lifetime is the navigation graph's, there is exactly
 * one of it, nothing injects it, and it cannot be reached from a layer that has no business posting
 * UI feedback. Only one message is kept - the newest wins - because two confirmations queued behind
 * each other is not something the user asked for.
 */
@Stable
class TransientMessages {

    var pending: UiMessage? by mutableStateOf(null)
        private set

    fun show(message: UiMessage) {
        pending = message
    }

    /** Called by the screen that has shown it, so it is not shown twice. */
    fun consume() {
        pending = null
    }
}
