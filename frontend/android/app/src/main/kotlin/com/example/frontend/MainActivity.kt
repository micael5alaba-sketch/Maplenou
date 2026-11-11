package com.example.frontend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createHighImportanceChannel()
    }

    /**
     * Canal "maplenou_high" (importance haute) : sans lui, Android classe les notifications push
     * en importance par defaut (icone dans la barre, pas de pop-up). Le backend l'utilise via
     * AndroidNotification.setChannelId (FcmService).
     */
    private fun createHighImportanceChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            "maplenou_high",
            "Notifications Maplenou",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Commandes, messages, livraisons et nouveautes"
            enableVibration(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
