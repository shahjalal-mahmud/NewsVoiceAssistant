package com.appriyo.newsvoiceassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.appriyo.newsvoiceassistant.service.NewsVoiceService
import com.appriyo.newsvoiceassistant.ui.screen.DashboardScreen
import com.appriyo.newsvoiceassistant.ui.screen.PermissionScreen
import com.appriyo.newsvoiceassistant.ui.theme.NewsVoiceAssistantTheme
import com.appriyo.newsvoiceassistant.util.OverlayPermissionHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsVoiceAssistantTheme {
                var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.CHECKING) }
                var serviceStarted by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // Check if overlay permission is available on this device
                    if (!OverlayPermissionHelper.isOverlayPermissionAvailable(this@MainActivity)) {
                        permissionState = PermissionState.UNAVAILABLE
                        return@LaunchedEffect
                    }

                    // Check current permission status
                    if (OverlayPermissionHelper.hasPermission(this@MainActivity)) {
                        permissionState = PermissionState.GRANTED
                    } else {
                        permissionState = PermissionState.REQUEST
                    }
                }

                when (permissionState) {
                    PermissionState.CHECKING -> {
                        // Show loading or nothing
                    }
                    PermissionState.REQUEST -> {
                        PermissionScreen(
                            onGranted = {
                                permissionState = PermissionState.GRANTED
                            },
                            onUnavailable = {
                                permissionState = PermissionState.UNAVAILABLE
                            }
                        )
                    }
                    PermissionState.GRANTED -> {
                        // Start service automatically when permission granted
                        LaunchedEffect(permissionState) {
                            if (!serviceStarted) {
                                startService(Intent(this@MainActivity, NewsVoiceService::class.java).apply {
                                    action = NewsVoiceService.ACTION_START
                                })
                                serviceStarted = true
                            }
                        }

                        DashboardScreen(
                            onStartService = {
                                startService(Intent(this@MainActivity, NewsVoiceService::class.java).apply {
                                    action = NewsVoiceService.ACTION_START
                                })
                            },
                            onStopService = {
                                stopService(Intent(this@MainActivity, NewsVoiceService::class.java))
                            }
                        )
                    }
                    PermissionState.UNAVAILABLE -> {
                        // Fallback mode - notification only
                        LaunchedEffect(permissionState) {
                            if (!serviceStarted) {
                                startService(Intent(this@MainActivity, NewsVoiceService::class.java).apply {
                                    action = NewsVoiceService.ACTION_START
                                })
                                serviceStarted = true
                            }
                        }

                        DashboardScreen(
                            onStartService = {
                                startService(Intent(this@MainActivity, NewsVoiceService::class.java).apply {
                                    action = NewsVoiceService.ACTION_START
                                })
                            },
                            onStopService = {
                                stopService(Intent(this@MainActivity, NewsVoiceService::class.java))
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed class PermissionState {
    object CHECKING : PermissionState()
    object REQUEST : PermissionState()
    object GRANTED : PermissionState()
    object UNAVAILABLE : PermissionState()
}