package com.griff.keeper.presentation.drawer

import com.griff.keeper.presentation.common.format.formatted

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.HeroStatTile
import com.griff.keeper.presentation.common.currency.CurrencyChangeStep
import com.griff.keeper.presentation.common.currency.CurrencyConversionConfirmDialog
import com.griff.keeper.presentation.common.currency.CurrencyConversionPreviewDialog
import com.griff.keeper.presentation.common.currency.CurrencyConversionProgressDialog
import com.griff.keeper.presentation.common.currency.CurrencyPickerDialog
import com.griff.keeper.presentation.common.currency.ExchangeRateDialog
import com.griff.keeper.presentation.common.currency.displayNameRes
import com.griff.keeper.presentation.common.locale.AppLanguage
import com.griff.keeper.presentation.common.locale.LanguagePickerDialog
import com.griff.keeper.presentation.common.rememberUrlOpener
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/** Destinations reachable from the navigation drawer. */
enum class DrawerDestination {
    SUBSCRIPTIONS,
    OBLIGATIONS,
    STATISTICS,
    REMINDERS,
    DATA_TRANSFER,
    ABOUT,
}

/**
 * Drawer content: app identity, primary destinations and the real build information.
 *
 * The selected destination uses the primary container rather than the Material default (the neutral
 * secondary container), because navigation selection is one of the few places where the brand accent
 * carries meaning. Its indicator is nearly square instead of the Material pill, which suits the
 * app's technical look.
 */
@Composable
internal fun AppDrawerContent(
    selected: DrawerDestination,
    appVersion: AppVersion?,
    totals: SubscriptionTotals,
    upcomingReminderCount: Int,
    language: AppLanguage,
    currency: Currency,
    currencyChangeStep: CurrencyChangeStep,
    onSelect: (DrawerDestination) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCurrencySelected: (Currency) -> Unit,
    onRateInputChanged: (String) -> Unit,
    onRateConfirmed: () -> Unit,
    onPreviewConfirmed: () -> Unit,
    onConversionConfirmed: () -> Unit,
    onCurrencyChangeCancelled: () -> Unit,
) {
    // Deliberately `remember` and not `rememberSaveable`: choosing a language recreates the
    // activity, and saved state would bring the dialog back up on top of the newly translated UI.
    var isPickingLanguage by remember { mutableStateOf(false) }

    // Rotation-safe state (the conversion flow itself) lives in the ViewModel; only "is the picker
    // open" is local, exactly like the language picker above - picking a currency never recreates the
    // activity, so there is no reason for this flag to survive one.
    var isPickingCurrency by remember { mutableStateOf(false) }

    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GriffGradients.accent())
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(HeaderTileCorner))
                            .background(GriffGradients.OnAccent.copy(alpha = 0.94f))
                            .padding(Spacing.ExtraSmall),
                    ) {
                        Image(
                            // The full lockup, crest and wordmark together - this tile is the one
                            // place in the app with room for the whole mark. The night variant is
                            // cyan and unreadable on navy, so the tile stays on the navy/gold mark
                            // regardless of theme.
                            painter = painterResource(R.drawable.ic_griff_emblem_on_navy),
                            contentDescription = null,
                            modifier = Modifier.height(HeaderEmblemHeight),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                        Text(
                            text = stringResource(R.string.app_display_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = GriffGradients.OnAccent,
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GriffGradients.OnAccent.copy(alpha = 0.85f),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    HeroStatTile(
                        label = stringResource(R.string.drawer_monthly_label),
                        value = totals.monthly.formatted(),
                        modifier = Modifier.weight(1f),
                    )
                    HeroStatTile(
                        label = stringResource(R.string.drawer_yearly_label),
                        value = totals.yearly.formatted(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val itemShape = GriffShapes.Container

            // Scrollable so a small screen (or a long list of destinations) never pushes the footer
            // below the visible drawer - the footer stays pinned, and only this middle section yields.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(Spacing.Small))

                DrawerItem(
                    label = stringResource(R.string.drawer_subscriptions),
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    selected = selected == DrawerDestination.SUBSCRIPTIONS,
                    onClick = { onSelect(DrawerDestination.SUBSCRIPTIONS) },
                    shape = itemShape,
                )
                DrawerItem(
                    label = stringResource(R.string.drawer_obligations),
                    icon = Icons.Default.VerifiedUser,
                    selected = selected == DrawerDestination.OBLIGATIONS,
                    onClick = { onSelect(DrawerDestination.OBLIGATIONS) },
                    shape = itemShape,
                )
                DrawerItem(
                    label = stringResource(R.string.drawer_statistics),
                    icon = Icons.Default.InsertChartOutlined,
                    selected = selected == DrawerDestination.STATISTICS,
                    onClick = { onSelect(DrawerDestination.STATISTICS) },
                    shape = itemShape,
                )
                DrawerItem(
                    label = stringResource(R.string.drawer_reminders),
                    icon = Icons.Default.Notifications,
                    selected = selected == DrawerDestination.REMINDERS,
                    onClick = { onSelect(DrawerDestination.REMINDERS) },
                    shape = itemShape,
                    badgeCount = upcomingReminderCount,
                )
                DrawerItem(
                    label = stringResource(R.string.drawer_data_transfer),
                    icon = Icons.Default.ImportExport,
                    selected = selected == DrawerDestination.DATA_TRANSFER,
                    onClick = { onSelect(DrawerDestination.DATA_TRANSFER) },
                    shape = itemShape,
                )
                // Below the divider: settings and the app talking about itself, rather than the
                // destinations the user actually works in.
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = Spacing.ExtraLarge,
                        vertical = Spacing.Small,
                    ),
                )

                val languageName = stringResource(language.displayNameRes)
                val languageItemDescription = stringResource(
                    R.string.language_item_description,
                    stringResource(R.string.drawer_language),
                    languageName,
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_language)) },
                    icon = { Icon(Icons.Default.Language, contentDescription = null) },
                    // The current language sits in the badge slot, so the drawer answers "which language
                    // am I in" without the user opening anything.
                    badge = {
                        Text(
                            text = languageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    selected = false,
                    onClick = { isPickingLanguage = true },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        // One label and one value read as one control; without this the label and the
                        // badge are announced as two unrelated pieces of text.
                        .clearAndSetSemantics {
                            contentDescription = languageItemDescription
                        },
                    shape = itemShape,
                )

                val currencyItemDescription = stringResource(
                    R.string.currency_item_description,
                    stringResource(R.string.drawer_currency),
                    currency.code,
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_currency)) },
                    icon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null) },
                    // The code, not the display name, sits in the badge: "PLN" and "EUR" are what the
                    // rest of the app shows next to an amount, so the drawer answers the same question the
                    // same way.
                    badge = {
                        Text(
                            text = currency.code,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    selected = false,
                    onClick = { isPickingCurrency = true },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .clearAndSetSemantics {
                            contentDescription = currencyItemDescription
                        },
                    shape = itemShape,
                )

                // Last in the list: not a place the user works, but where the app explains itself.
                DrawerItem(
                    label = stringResource(R.string.drawer_about),
                    icon = Icons.Outlined.Info,
                    selected = selected == DrawerDestination.ABOUT,
                    onClick = { onSelect(DrawerDestination.ABOUT) },
                    shape = itemShape,
                )
            }

            HorizontalDivider()

            val openUrl = rememberUrlOpener()
            val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    Text(
                        text = stringResource(R.string.app_display_name),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.drawer_version_build,
                            appVersion?.name ?: "",
                            appVersion?.code ?: 0L,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
                        modifier = Modifier.clickable { openUrl(privacyPolicyUrl) },
                    ) {
                        Text(
                            text = stringResource(R.string.drawer_privacy_policy),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(FooterLinkIconSize),
                        )
                    }
                    Text(
                        text = stringResource(R.string.drawer_local_data),
                        style = MaterialTheme.typography.labelMedium,
                        color = GriffTheme.colors.success,
                    )
                }
            }
        }
    }

    if (isPickingLanguage) {
        LanguagePickerDialog(
            selected = language,
            onSelect = { picked ->
                // Closed before the choice is applied, so the state that gets saved across the
                // activity recreation is "no dialog".
                isPickingLanguage = false
                // Re-picking the language that is already active would recreate the activity for
                // nothing.
                if (picked != language) onLanguageSelected(picked)
            },
            onDismiss = { isPickingLanguage = false },
        )
    }

    if (isPickingCurrency) {
        CurrencyPickerDialog(
            selected = currency,
            onSelect = { picked ->
                isPickingCurrency = false
                // Re-picking the active currency would ask for a rate to convert it into itself.
                if (picked != currency) onCurrencySelected(picked)
            },
            onDismiss = { isPickingCurrency = false },
        )
    }

    val isCurrencyChangeBusy = currencyChangeStep is CurrencyChangeStep.Converting
    when (val step = currencyChangeStep) {
        CurrencyChangeStep.None -> Unit

        is CurrencyChangeStep.EnteringRate -> ExchangeRateDialog(
            fromName = stringResource(step.from.displayNameRes),
            toName = stringResource(step.to.displayNameRes),
            rateInput = step.rateInput,
            error = step.error,
            isBusy = isCurrencyChangeBusy,
            onRateChange = onRateInputChanged,
            onContinue = onRateConfirmed,
            onDismiss = onCurrencyChangeCancelled,
        )

        is CurrencyChangeStep.Previewing -> CurrencyConversionPreviewDialog(
            preview = step.preview,
            isBusy = isCurrencyChangeBusy,
            onContinue = onPreviewConfirmed,
            onDismiss = onCurrencyChangeCancelled,
        )

        is CurrencyChangeStep.Confirming -> CurrencyConversionConfirmDialog(
            preview = step.preview,
            isBusy = isCurrencyChangeBusy,
            onConfirm = onConversionConfirmed,
            onCreateBackup = null,
            onDismiss = onCurrencyChangeCancelled,
        )

        is CurrencyChangeStep.Converting -> CurrencyConversionProgressDialog()
    }
}

/**
 * One destination row.
 *
 * The active row carries the navy gradient in place of Material's flat `primaryContainer` - the same
 * treatment as every other "you are here" moment in the redesign - so it needs its own [Row] rather
 * than [NavigationDrawerItem]'s color API, which only accepts a solid [androidx.compose.ui.graphics.Color].
 * An inactive row stays on stock [NavigationDrawerItem] so it keeps Material's built-in ripple and
 * touch target handling for free.
 */
@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    badgeCount: Int = 0,
) {
    if (!selected) {
        NavigationDrawerItem(
            label = { Text(label) },
            icon = { Icon(icon, contentDescription = null) },
            badge = if (badgeCount > 0) {
                { DrawerBadge(count = badgeCount) }
            } else {
                null
            },
            selected = false,
            onClick = onClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            shape = shape,
        )
        return
    }

    Row(
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .fillMaxWidth()
            .height(DrawerItemHeight)
            .clip(shape)
            .background(GriffGradients.accent())
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        Icon(icon, contentDescription = null, tint = GriffGradients.OnAccent)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = GriffGradients.OnAccent,
            modifier = Modifier.weight(1f),
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .clip(GriffShapes.Pill)
                    .background(GriffGradients.OnAccent.copy(alpha = 0.25f))
                    .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall / 2),
            ) {
                Text(
                    text = badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = GriffGradients.OnAccent,
                )
            }
        }
    }
}

@Composable
private fun DrawerBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(GriffShapes.Pill)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall / 2),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private val DrawerItemHeight = 56.dp

/**
 * Shrunk to sit comfortably next to the two-line title block without dominating the header; the
 * emblem's own aspect ratio (from [R.drawable.ic_griff_emblem]) determines its width.
 */
private val HeaderEmblemHeight = 52.dp
private val HeaderTileCorner = 9.dp
private val FooterLinkIconSize = 15.dp

@ThemePreviews
@Composable
private fun AppDrawerContentPreview() {
    GriffThemePreview {
        AppDrawerContent(
            selected = DrawerDestination.SUBSCRIPTIONS,
            appVersion = AppVersion(name = "1.0.0", code = 1L),
            totals = SubscriptionTotals(Money.ofUnits(65, 48), Money.ofUnits(785, 76), 2),
            upcomingReminderCount = 3,
            language = AppLanguage.ENGLISH,
            currency = Currency.PLN,
            currencyChangeStep = CurrencyChangeStep.None,
            onSelect = {},
            onLanguageSelected = {},
            onCurrencySelected = {},
            onRateInputChanged = {},
            onRateConfirmed = {},
            onPreviewConfirmed = {},
            onConversionConfirmed = {},
            onCurrencyChangeCancelled = {},
        )
    }
}
