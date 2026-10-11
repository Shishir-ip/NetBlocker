package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NetBlockerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NetBlockerViewModel by viewModels {
        NetBlockerViewModel.provideFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Optimize for 120Hz, 90Hz, and 60Hz displays
        optimizeDisplayRefreshRate()

        setContent {
            MyApplicationTheme {
                // Request POST_NOTIFICATIONS on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* Handled */ }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun optimizeDisplayRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                @Suppress("DEPRECATION")
                val modes = display?.supportedModes
                val highestRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null && highestRefreshRateMode.refreshRate > 60f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestRefreshRateMode.modeId
                    window.attributes = params
                }
            } catch (_: Exception) {
                // Fallback gracefully on devices that do not expose display modes
            }
        }
    }
}
