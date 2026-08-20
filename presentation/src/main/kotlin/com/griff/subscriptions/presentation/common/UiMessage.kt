package com.griff.subscriptions.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A message that a screen wants to show, kept as a resource reference so ViewModels stay free of
 * localized text.
 */
data class UiMessage(
    @param:StringRes val textRes: Int,
    val formatArgs: List<Any> = emptyList(),
)

@Composable
fun UiMessage.resolve(): String =
    if (formatArgs.isEmpty()) {
        stringResource(textRes)
    } else {
        stringResource(textRes, *formatArgs.toTypedArray())
    }
