package com.griff.keeper.presentation.common.format

import androidx.compose.runtime.Composable
import com.griff.keeper.domain.model.Money
import com.griff.keeper.presentation.common.currency.LocalAppCurrency

/**
 * Formats this amount in the app's active currency ([LocalAppCurrency]) and the active language's
 * locale.
 *
 * The call sites that used to read `MoneyFormatter.format(money)` were silently formatting every
 * amount as PLN, because that is [MoneyFormatter.format]'s default parameter - harmless while PLN was
 * the only currency the app had, wrong the moment a second one exists. This is the replacement: one
 * composable extension instead of every call site naming the active currency itself.
 */
@Composable
internal fun Money.formatted(): String = MoneyFormatter.format(this, LocalAppCurrency.current, currentLocale())
