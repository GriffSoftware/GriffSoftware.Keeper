package com.griff.keeper.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * What a message means, which decides the icon and the status color it is shown with.
 *
 * Severity is part of the message and not of the call site, so a ViewModel says "this failed"
 * instead of picking a color.
 */
enum class MessageSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

/**
 * A message that a screen wants to show, kept as a resource reference so ViewModels stay free of
 * localized text.
 */
data class UiMessage(
    @param:StringRes val textRes: Int,
    val formatArgs: List<Any> = emptyList(),
    val severity: MessageSeverity = MessageSeverity.INFO,
)

/** A [UiMessage] with its text resolved, ready to be handed to a snackbar host. */
data class ResolvedMessage(
    val text: String,
    val severity: MessageSeverity,
)

@Composable
fun UiMessage.resolve(): ResolvedMessage =
    ResolvedMessage(
        text = if (formatArgs.isEmpty()) {
            stringResource(textRes)
        } else {
            stringResource(textRes, *formatArgs.toTypedArray())
        },
        severity = severity,
    )
