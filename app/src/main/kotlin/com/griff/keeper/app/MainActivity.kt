package com.griff.keeper.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.griff.keeper.presentation.navigation.GriffKeeperApp
import com.griff.keeper.presentation.theme.GriffKeeperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Single activity hosting the Compose navigation graph. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
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
