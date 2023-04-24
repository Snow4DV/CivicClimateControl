package ru.snowadv.civic_climate_control;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class AutostartActivity extends AppCompatActivity {

    private static final String TAG = "AutostartOverlActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startService(this);
        overridePendingTransition(0, 0);
        finish();
        super.onCreate(savedInstanceState);
    }

    private void startService(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "floating_panel_enabled", false)) {
            Toast.makeText(this, R.string.climate_autostarted, Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(new Intent(context, ClimateOverlayService.class));
            } else {
                context.startService(new Intent(context, ClimateOverlayService.class));
            }
        } else {
            Toast.makeText(this, R.string.overlay_disabled, Toast.LENGTH_SHORT).show();
        }

        Log.i(TAG, "service started");
    }
}