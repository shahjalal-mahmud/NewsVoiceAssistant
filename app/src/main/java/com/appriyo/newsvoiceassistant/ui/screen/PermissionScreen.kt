package com.appriyo.newsvoiceassistant.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appriyo.newsvoiceassistant.util.OverlayPermissionHelper

@Composable
fun PermissionScreen(onGranted: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (OverlayPermissionHelper.hasPermission(context)) {
            onGranted()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Overlay permission required")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            OverlayPermissionHelper.requestPermission(context)
        }) {
            Text("Grant Permission")
        }
    }
}