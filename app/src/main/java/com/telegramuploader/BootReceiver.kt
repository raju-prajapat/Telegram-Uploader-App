package com.telegramuploader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {

        if (
            intent?.action != Intent.ACTION_BOOT_COMPLETED
        ) {
            return
        }

        val enabled =
            context
                .getSharedPreferences(
                    "automation_settings",
                    Context.MODE_PRIVATE
                )
                .getBoolean(
                    "auto_screenshot_upload",
                    false
                )

        if (!enabled) {
            return
        }

        val serviceIntent =
            Intent(
                context,
                ScreenshotUploadService::class.java
            ).apply {
                action =
                    ScreenshotUploadService.ACTION_START
            }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            context.startForegroundService(
                serviceIntent
            )
        } else {
            context.startService(
                serviceIntent
            )
        }
    }
}
