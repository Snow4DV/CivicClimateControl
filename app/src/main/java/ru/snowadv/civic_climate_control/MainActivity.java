package ru.snowadv.civic_climate_control;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import ru.snowadv.civic_climate_control.overlay.LayoutClimateOverlay;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(getApplicationContext(),SettingsActivity.class);
        startActivity(intent);

        if(checkOverlayPermission()) {
            if (Build.VERSION.SDK_INT >= 26) {
                getApplicationContext().startForegroundService(new Intent(getApplicationContext(), LayoutClimateOverlay.class));
            } else {
                startService(new Intent(getApplicationContext(), LayoutClimateOverlay.class));
            }
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }

        if(checkPermission(0)) {

        }

    }



    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
        {


            if (!Settings.canDrawOverlays(this))
            {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return false;
            }
            else
            {
                return true;
            }
        }else{
            return true;
        }
    }

    private boolean checkPermission(int requestCode) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
                == PackageManager.PERMISSION_DENIED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.FOREGROUND_SERVICE }, requestCode);
            return false;
        } else {
            Toast.makeText(this, "Foreground service granted", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}