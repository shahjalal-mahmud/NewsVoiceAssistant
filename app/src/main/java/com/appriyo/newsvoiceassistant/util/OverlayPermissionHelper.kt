package com.appriyo.newsvoiceassistant.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OverlayPermissionHelper {

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            // For devices below Android 6.0, overlay permission is granted by default
            true
        }
    }

    fun isOverlayPermissionAvailable(context: Context): Boolean {
        // Check if this device supports overlay permission
        // Some devices (like some Xiaomi, Huawei) might not support it properly
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
                true // If no exception, permission system is available
            } else {
                true
            }
        } catch (e: Exception) {
            false // Overlay permission feature not available
        }
    }

    fun requestPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}