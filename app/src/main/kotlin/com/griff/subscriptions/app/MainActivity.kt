package com.griff.subscriptions.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.griff.subscriptions.presentation.navigation.GriffSubscriptionsApp
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single activity hosting the Compose navigation graph. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GriffSubscriptionsTheme {
                // The window background, so the app matches the theme before and behind Compose.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GriffSubscriptionsApp()
                }
            }
        }
    }
}
