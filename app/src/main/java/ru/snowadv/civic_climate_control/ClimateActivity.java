package ru.snowadv.civic_climate_control;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.Objects;

import ru.snowadv.civic_climate_control.databinding.ActivityClimateBinding;

public class ClimateActivity extends AppCompatActivity {


    private ActivityClimateBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideTitleAndNotificationBars();

        View rootView = initViewBinding();

        setContentView(rootView);
        initInterfaceListeners();
    }

    @NonNull
    private View initViewBinding() {
        binding = ActivityClimateBinding.inflate(getLayoutInflater());
        return binding.getRoot();
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
}