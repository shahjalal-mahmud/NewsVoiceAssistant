package com.appriyo.newsvoiceassistant.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appriyo.newsvoiceassistant.util.OverlayPermissionHelper

@Composable
fun PermissionScreen(
    onGranted: () -> Unit,
    onUnavailable: () -> Unit
) {
    val context = LocalContext.current
    var showSkipButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Check permission when screen is shown
        if (OverlayPermissionHelper.hasPermission(context)) {
            onGranted()
        }
    }

    // Show skip button after 3 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        showSkipButton = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Permission Required",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Overlay Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "To show the floating bubble while using other apps, we need permission to draw over other apps.\n\n" +
                    "Without this permission, the app will still work using notification controls only.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    OverlayPermissionHelper.requestPermission(context)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant Permission")
            }

            if (showSkipButton) {
                OutlinedButton(
                    onClick = {
                        onUnavailable()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with Notifications Only")
                }

                Text(
                    text = "You can always enable this later in app settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}