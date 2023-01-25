package ru.snowadv.civic_climate_control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ClimateAutoStart extends BroadcastReceiver
{
    public void onReceive(Context context, Intent arg1)
    {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(new Intent(context, ClimateService.class));
        } else {
            context.startService(new Intent(context, ClimateService.class));
        }
        Log.i("Autostart", "started");
    }
}