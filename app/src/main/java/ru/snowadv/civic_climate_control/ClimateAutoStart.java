package ru.snowadv.civic_climate_control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class ClimateAutoStart extends BroadcastReceiver
{

    // TODO: fix it
    private static final String TAG = "CivicClimateAutostart";

    public void onReceive(Context context, Intent arg1)
    {
        context.startActivity(new Intent(context, ClimateActivity.class));
        startService(context);

    }

    private void startService(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "floating_panel_enabled", false)) {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(new Intent(context, ClimateOverlayService.class));
            } else {
                context.startService(new Intent(context, ClimateOverlayService.class));
            }
        }

        Log.i(TAG, "service started");
    }

}