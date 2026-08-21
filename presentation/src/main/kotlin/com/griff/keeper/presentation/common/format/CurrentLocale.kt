package com.griff.keeper.presentation.common.format

import androidx.compose.runtime.Composable
import java.util.Locale
import androidx.compose.ui.text.intl.Locale as ComposeLocale

/**
 * The locale the composition is resolving text against.
 *
 * Read through Compose rather than from [Locale.getDefault] so that composition *observes* it:
 * anything that depends on the language is recomposed when the language changes instead of keeping
 * whatever was true when it first ran. The language tag is the bridge to `java.util`, which is what
 * `DecimalFormat` and `DateTimeFormatter` take.
 *
 * Only needed where a composable formats something itself. The formatter objects take a [Locale]
 * parameter and default it to the process locale, which `MainActivity` keeps in step with the app's
 * language - a locale change recreates the activity, so the whole composition is rebuilt anyway.
 */
@Composable
internal fun currentLocale(): Locale = Locale.forLanguageTag(ComposeLocale.current.toLanguageTag())
