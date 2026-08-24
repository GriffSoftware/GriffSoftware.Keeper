package com.griff.keeper.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import kotlinx.coroutines.delay

/**
 * The Compose splash, shown for a fixed beat after the native cold-start screen
 * (`Theme.GriffKeeper.Splash` in `app/src/main/res/values/themes.xml`) hands off.
 *
 * There is no loading state to reflect - nothing here waits on data - so [onFinished] fires after a
 * short fixed delay rather than a real progress signal; the delay exists purely so the brand moment
 * registers instead of flashing past.
 */
@Composable
fun SplashRoute(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SplashDurationMillis)
        onFinished()
    }
    SplashScreen()
}

@Composable
internal fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GriffGradients.accent()),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(GriffGradients.sheen()))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(Spacing.Huge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(EmblemTileCorner))
                        .background(GriffGradients.OnAccent.copy(alpha = 0.96f))
                        .padding(Spacing.ExtraLarge),
                ) {
                    Image(
                        // Always the navy/gold mark, never the night variant: cyan does not read
                        // against this navy background. The icon-only crop, not the full lockup
                        // with the wordmark - a tile this size has no room for both.
                        painter = painterResource(R.drawable.ic_griff_emblem_icon),
                        contentDescription = null,
                        modifier = Modifier.height(EmblemHeight),
                    )
                }

                Column(
                    modifier = Modifier.padding(top = Spacing.ExtraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Text(
                        text = stringResource(R.string.app_display_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = GriffGradients.OnAccent,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.splash_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GriffGradients.OnAccent.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                LinearProgressIndicator(
                    color = GriffGradients.OnAccent,
                    trackColor = GriffGradients.OnAccent.copy(alpha = 0.22f),
                    modifier = Modifier.width(ProgressWidth),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = GriffGradients.OnAccent.copy(alpha = 0.75f),
                        modifier = Modifier.height(LockIconSize),
                    )
                    Text(
                        text = stringResource(R.string.splash_local_data),
                        style = MaterialTheme.typography.labelMedium,
                        color = GriffGradients.OnAccent.copy(alpha = 0.75f),
                    )
                }
                Text(
                    text = stringResource(R.string.about_copyright).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = GriffGradients.OnAccent.copy(alpha = 0.55f),
                )
            }
        }
    }
}

/** Long enough for the brand moment to register, short enough not to feel like a wait. */
private const val SplashDurationMillis = 700L

private val EmblemHeight = 64.dp
private val EmblemTileCorner = 16.dp
private val ProgressWidth = 96.dp
private val LockIconSize = 14.dp

@ThemePreviews
@Composable
private fun SplashScreenPreview() {
    GriffThemePreview {
        SplashScreen()
    }
}
