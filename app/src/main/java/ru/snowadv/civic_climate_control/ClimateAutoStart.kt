package ru.snowadv.civic_climate_control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import ru.snowadv.civic_climate_control.overlay.LayoutClimateOverlay
import ru.snowadv.civic_climate_control.overlay.NotificationClimateOverlay

class ClimateAutoStart : BroadcastReceiver() {

    private val TAG = "CivicClimateAutostart"
    override fun onReceive(context: Context, arg1: Intent) {
        startService(context)
    }

    private fun startService(context: Context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "floating_panel_enabled", false
            )
        ) {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(Intent(context, LayoutClimateOverlay::class.java))
            } else {
                context.startService(Intent(context, LayoutClimateOverlay::class.java))
            }
        }
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "notifications_enabled", false
            )
        ) {
            context.startService(Intent(context, NotificationClimateOverlay::class.java))
        }
        Log.i(TAG, "services started")
    }

}