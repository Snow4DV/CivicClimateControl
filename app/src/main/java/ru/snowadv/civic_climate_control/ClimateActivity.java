package ru.snowadv.civic_climate_control;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Objects;

import ru.snowadv.civic_climate_control.databinding.ActivityClimateBinding;

public class ClimateActivity extends AppCompatActivity {


    private ActivityClimateBinding binding;
    private SharedPreferences settingsPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideTitleAndNotificationBars();

        View rootView = initViewBinding();

        setContentView(rootView);
        initFields();
        initInterfaceListeners();

        restartServiceIfDead();
    }

    @NonNull
    private View initViewBinding() {
        binding = ActivityClimateBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    private void initFields() {
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }


    private void initInterfaceListeners() {
        binding.settingsButton.setOnClickListener(view -> openSettingsActivity());
    }

    private void hideTitleAndNotificationBars() {
        Objects.requireNonNull(getSupportActionBar()).hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void openSettingsActivity() {
        Intent settingsIntent = new Intent(ClimateActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void restartServiceIfDead() {
        if (settingsPreferences.getBoolean("floating_panel_enabled", false)) {
            startService(new Intent(this, ClimateService.class));
        }
    }




}