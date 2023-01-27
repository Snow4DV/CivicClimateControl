package ru.snowadv.civic_climate_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

public class ClimateAutoStart extends BroadcastReceiver
{

    private static final String TAG = "CivicClimateAutostart";

    public void onReceive(Context context, Intent arg1)
    {
        context.startActivity(new Intent(context, ClimateActivity.class));
        Toast.makeText(context, "YEEEAAH", Toast.LENGTH_SHORT).show();
        startService(context);

    }

    private void startService(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("floating_panel_enabled", false)) {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(new Intent(context, ClimateService.class));
            } else {
                context.startService(new Intent(context, ClimateService.class));
            }
        }

        Log.i(TAG, "service started");
    }

}