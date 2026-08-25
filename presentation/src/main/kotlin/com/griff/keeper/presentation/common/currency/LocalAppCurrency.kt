package com.griff.keeper.presentation.common.currency

import androidx.compose.runtime.compositionLocalOf
import com.griff.keeper.domain.model.Currency

/**
 * The app's single active currency, provided once near the root of the composition (see
 * `GriffKeeperApp`) and read wherever a screen formats a
 * [com.griff.keeper.domain.model.Money] value.
 *
 * A `CompositionLocal` rather than a parameter threaded through every screen: money is formatted in
 * roughly thirty places across five feature screens, all of which want the same value at the same
 * time - exactly the case a `CompositionLocal` exists for, the same way `LocalConfiguration` already
 * carries the active language.
 */
val LocalAppCurrency = compositionLocalOf { Currency.Default }
