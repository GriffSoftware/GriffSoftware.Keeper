package com.griff.keeper.presentation.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a preview twice, once per theme, so the light and the dark variant of a screen can be
 * compared side by side in the Android Studio preview pane.
 */
@Preview(name = "Griff Light", group = "Griff", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Griff Dark", group = "Griff", uiMode = Configuration.UI_MODE_NIGHT_YES)
internal annotation class ThemePreviews

/** [ThemePreviews] for screens that scroll well past the height of a phone. */
@Preview(name = "Griff Light", group = "Griff", uiMode = Configuration.UI_MODE_NIGHT_NO, heightDp = 1400)
@Preview(name = "Griff Dark", group = "Griff", uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 1400)
internal annotation class TallThemePreviews

/**
 * Preview host: applies the theme and paints the window background, which components that do not
 * bring their own `Scaffold` need in order to look the way they do in the app.
 */
@Composable
internal fun GriffThemePreview(content: @Composable () -> Unit) {
    GriffKeeperTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
