package com.griff.keeper.presentation.about

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.component.GriffCard
import com.griff.keeper.presentation.common.component.GriffHeroCard
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.MinTouchTarget
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.TallThemePreviews
import kotlinx.coroutines.launch

/**
 * Entry point of the About screen.
 *
 * No ViewModel: the screen has no asynchronous state and no business logic. The one fact it shows
 * that is not a string resource - the version of the running build - already exists in the
 * composition root, so it is handed down instead of being fetched again through a use case of its
 * own. What does live here is the platform: the two interactions (write to us, copy the address) are
 * an `Intent` and the clipboard, which the screen below never touches.
 */
@Composable
fun AboutRoute(
    appVersion: AppVersion,
    onOpenDrawer: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<UiMessage?>(null) }

    val address = stringResource(R.string.about_contact_email)
    val subject = stringResource(R.string.about_contact_email_subject)
    val body = stringResource(R.string.about_contact_email_body, appVersion.name, appVersion.code)
    val clipLabel = stringResource(R.string.about_contact_email_clip_label)

    AboutScreen(
        appVersion = appVersion,
        message = message,
        onOpenDrawer = onOpenDrawer,
        onEmailClick = {
            val launched = context.startSupportEmail(
                address = address,
                subject = subject,
                body = body,
            )
            if (!launched) {
                message = UiMessage(
                    textRes = R.string.about_contact_email_no_app,
                    severity = MessageSeverity.WARNING,
                )
            }
        },
        onCopyEmail = {
            // Android 13 and later confirm a copy by itself, but the app still says so: the minimum
            // supported version is 8.1, where nothing else tells the user anything happened.
            scope.launch {
                clipboard.setClipEntry(
                    ClipData.newPlainText(clipLabel, address).toClipEntry(),
                )
                message = UiMessage(
                    textRes = R.string.about_contact_email_copied,
                    severity = MessageSeverity.SUCCESS,
                )
            }
        },
        onMessageShown = { message = null },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(
    appVersion: AppVersion,
    message: UiMessage?,
    onOpenDrawer: () -> Unit,
    onEmailClick: () -> Unit,
    onCopyEmail: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resolved = message?.resolve()

    LaunchedEffect(resolved) {
        if (resolved != null) {
            snackbarHostState.showMessage(resolved)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                        )
                    }
                },
            )
        },
        snackbarHost = { GriffSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        // A list rather than a fixed column: the content is longer than a small phone in landscape,
        // and longer still at the largest font scale. No contentPadding on the list itself - the
        // header runs full-bleed to the top edge, so each section pads itself instead.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraLarge),
        ) {
            item { AboutHeader() }
            item {
                FeaturesSection(modifier = Modifier.padding(horizontal = Spacing.Large))
            }
            item {
                PrivacyCard(modifier = Modifier.padding(horizontal = Spacing.Large))
            }
            item {
                ContactSection(
                    onEmailClick = onEmailClick,
                    onCopyEmail = onCopyEmail,
                    modifier = Modifier.padding(horizontal = Spacing.Large),
                )
            }
            item {
                VersionFooter(
                    appVersion = appVersion,
                    modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small),
                )
            }
        }
    }
}

/**
 * The app's identity and what it is for, in the two sentences a user needs to recognize it.
 *
 * The emblem sits on a white tile and always uses the navy/gold mark - the night variant is cyan
 * and does not read against the header's navy gradient, the same reasoning as the drawer header and
 * the splash screen.
 */
@Composable
private fun AboutHeader() {
    GriffHeroCard(
        shape = GriffShapes.HeroTopAttached,
        contentPadding = PaddingValues(horizontal = Spacing.Large, vertical = Spacing.Large),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(EmblemTileCorner))
                    .background(GriffGradients.OnAccent.copy(alpha = 0.94f))
                    .padding(Spacing.Medium),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_griff_emblem_on_navy),
                    contentDescription = stringResource(R.string.about_emblem_description),
                    modifier = Modifier.height(EmblemHeight),
                )
            }
            Text(
                text = stringResource(R.string.app_display_name),
                style = MaterialTheme.typography.headlineSmall,
                color = GriffGradients.OnAccent,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.about_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = GriffGradients.OnAccent.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * What the app does, one line per capability.
 *
 * Icons are the ones the corresponding screens and the drawer already use, so the list reads as a
 * map of the app rather than as decoration. Not a complete inventory of features - six lines the
 * user can take in at a glance.
 */
@Composable
private fun FeaturesSection(modifier: Modifier = Modifier) {
    AboutSection(title = stringResource(R.string.about_features_title), modifier = modifier) {
        AboutFeature.entries.forEach { feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = FeatureRowHeight),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(FeatureTileSize)
                        .clip(RoundedCornerShape(FeatureTileCorner))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(FeatureIconSize),
                    )
                }
                Text(
                    text = stringResource(feature.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * The one property of the app that is worth stating outright.
 *
 * Kept factual: where the data is and what the app does not need. No claim about how strong anything
 * is - that would be marketing dressed up as a guarantee.
 */
@Composable
private fun PrivacyCard(modifier: Modifier = Modifier) {
    GriffCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(BannerIconSize),
                )
                Text(
                    text = stringResource(R.string.about_privacy_title),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = stringResource(R.string.about_privacy_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * How to reach a human.
 *
 * The address is both a link and a piece of text to take away: tapping it opens a mail app, and the
 * copy button is there for the device that has none, or for the user who would rather write from
 * their computer. Either way the address stays on screen.
 */
@Composable
private fun ContactSection(
    onEmailClick: () -> Unit,
    onCopyEmail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val address = stringResource(R.string.about_contact_email)

    AboutSection(title = stringResource(R.string.about_contact_title), modifier = modifier) {
        Text(
            text = stringResource(R.string.about_contact_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        onClickLabel = stringResource(
                            R.string.about_contact_email_action,
                            address,
                        ),
                        role = Role.Button,
                        onClick = onEmailClick,
                    )
                    .defaultMinSize(minHeight = MinTouchTarget)
                    .padding(vertical = Spacing.Small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(FeatureIconSize),
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onCopyEmail) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.about_contact_email_copy),
                )
            }
        }
    }
}

/** The real build, the same numbers the drawer shows, plus who made it. */
@Composable
private fun VersionFooter(appVersion: AppVersion, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        HorizontalDivider(modifier = Modifier.padding(bottom = Spacing.Large))
        Text(
            text = stringResource(R.string.app_display_name),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.about_version, appVersion.name, appVersion.code),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.about_copyright),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Section container, matching the cards the statistics screen groups its content with. */
@Composable
private fun AboutSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GriffCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

/** The six capabilities the About screen names, in the order the drawer lists their screens. */
private enum class AboutFeature(
    val icon: ImageVector,
    val labelRes: Int,
) {
    SUBSCRIPTIONS(Icons.AutoMirrored.Filled.ReceiptLong, R.string.about_feature_subscriptions),
    OBLIGATIONS(Icons.Default.VerifiedUser, R.string.about_feature_obligations),
    REMINDERS(Icons.Default.Notifications, R.string.about_feature_reminders),
    STATISTICS(Icons.Default.InsertChartOutlined, R.string.about_feature_statistics),
    TAGS(Icons.Default.Label, R.string.about_feature_tags),
    BACKUP(Icons.Default.ImportExport, R.string.about_feature_backup),
}

/**
 * Opens a mail app with a message to support already addressed.
 *
 * `ACTION_SENDTO` with a `mailto:` URI rather than `ACTION_SEND`: the user wants to write an email,
 * not to share arbitrary content with whatever happens to accept it, and only mail clients answer
 * this intent. The body carries the version of the build and nothing else - no identifier, no
 * setting and no record from the database.
 *
 * @return false when the device has no mail app, which the caller turns into a snackbar; the address
 * itself stays on screen either way.
 */
private fun Context.startSupportEmail(
    address: String,
    subject: String,
    body: String,
): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null)).apply {
        // Some clients read the recipient from the extra rather than from the URI.
        putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** Large enough to be the app's face on the screen, small enough to leave the text room. */
private val EmblemHeight = 72.dp
private val EmblemTileCorner = 11.dp

private val FeatureIconSize = 18.dp
private val FeatureTileSize = 30.dp
private val FeatureTileCorner = 5.dp
private val BannerIconSize = 18.dp

/** Keeps the feature lines evenly spaced whatever the font scale does to their height. */
private val FeatureRowHeight = 40.dp

@TallThemePreviews
@Composable
private fun AboutScreenPreview() {
    GriffThemePreview {
        AboutScreen(
            appVersion = AppVersion(name = "1.0.0", code = 1L),
            message = null,
            onOpenDrawer = {},
            onEmailClick = {},
            onCopyEmail = {},
            onMessageShown = {},
        )
    }
}
