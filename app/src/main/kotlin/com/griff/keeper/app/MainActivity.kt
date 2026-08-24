package com.griff.keeper.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.os.ConfigurationCompat
import com.griff.keeper.presentation.navigation.GriffKeeperApp
import com.griff.keeper.presentation.theme.GriffKeeperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale

/**
 * Single activity hosting the Compose navigation graph.
 *
 * An [AppCompatActivity] rather than a `ComponentActivity` for one reason: below Android 13 it is
 * AppCompat, not the platform, that applies the per-app language, and it does so by wrapping this
 * activity's base context. Nothing else about the setup changes - Hilt, `setContent`, edge-to-edge,
 * navigation and the Compose theme are exactly as they were, and AppCompat draws no action bar
 * because the window theme is a `NoActionBar` one.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /**
     * Deep links that arrive while the activity is already running.
     *
     * The activity is `singleTop`, so tapping a reminder while the app is open delivers a new intent
     * instead of a second copy of the app; the navigation graph picks the intent up from here. The
     * intent the activity was *started* with is handled by `NavHost` itself, which is why only the
     * later ones are republished.
     */
    private val newIntents = MutableSharedFlow<Intent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Read-only view, built once: creating it inside composition would allocate on every frame. */
    private val deepLinkIntents: SharedFlow<Intent> = newIntents.asSharedFlow()

    /**
     * Keeps the JVM's default locale in step with the language the resources are resolving against.
     *
     * `java.time` and `java.text` know nothing about Android resources: a `DateTimeFormatter` or a
     * `DecimalFormat` reads [Locale.getDefault]. From Android 13 the platform updates it along with
     * the per-app locale, but below that AppCompat only reconfigures this activity's context, which
     * would leave the app showing English copy next to Polish month names. Done here rather than in
     * composition because it has to be true before the first frame - and it runs again after every
     * language change, since applying one recreates the activity.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        ConfigurationCompat.getLocales(resources.configuration)[0]
            ?.takeIf { it != Locale.getDefault() }
            ?.let(Locale::setDefault)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GriffKeeperTheme {
                // The window background, so the app matches the theme before and behind Compose.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GriffKeeperApp(deepLinkIntents = deepLinkIntents)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        newIntents.tryEmit(intent)
    }
}
