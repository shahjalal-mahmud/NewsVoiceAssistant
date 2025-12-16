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
                var granted by remember { mutableStateOf(false) }
                var serviceStarted by remember { mutableStateOf(false) }

                LaunchedEffect(granted) {
                    if (granted && !serviceStarted) {
                        startService(Intent(this@MainActivity, NewsVoiceService::class.java).apply {
                            action = NewsVoiceService.ACTION_START
                        })
                        serviceStarted = true
                    }
                }

                if (!granted) {
                    PermissionScreen {
                        granted = true
                    }
                } else {
                    DashboardScreen(
                        onStartService = {
                            startService(Intent(this@MainActivity, NewsVoiceService::class.java).apply {
                                action = NewsVoiceService.ACTION_START
                            })
                        },
                        onStopService = {
                            stopService(Intent(this@MainActivity, NewsVoiceService::class.java))
                        },
                        onShowBubble = {
                            if (OverlayPermissionHelper.hasPermission(this@MainActivity)) {
                                // Service will handle bubble automatically
                            } else {
                                OverlayPermissionHelper.requestPermission(this@MainActivity)
                            }
                        }
                    )
                }
            }
        }
    }
}