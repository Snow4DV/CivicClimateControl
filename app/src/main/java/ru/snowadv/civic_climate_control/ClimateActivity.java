package ru.snowadv.civic_climate_control;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.util.Objects;

import ru.snowadv.civic_climate_control.Adapter.AdapterService;
import ru.snowadv.civic_climate_control.Adapter.AdapterState;
import ru.snowadv.civic_climate_control.databinding.ActivityClimateBinding;

public class ClimateActivity extends AppCompatActivity implements ServiceConnection, AdapterService.OnNewStateReceivedListener {


    private static final String TAG = "ClimateActivity";
    private ActivityClimateBinding binding;
    private SharedPreferences settingsPreferences;
    private NotifierUtility notifierUtility;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notifierUtility = new NotifierUtility(this);
        hideTitleAndNotificationBars();
        View rootView = initViewBinding();
        setContentView(rootView);

        initFields();
        initInterfaceListeners();
        restartOverlayServiceIfNeeded();

        initAdapterService();
    }


    private void initAdapterService() {
        String adapterDeviceJson = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("adapter_name", null);
        SerializableUsbDevice adapterDevice = SerializableUsbDevice.fromJson(adapterDeviceJson);
        AdapterService.getAccessAndBindService(this, adapterDevice, this);
    }

    public void onResume() {
        super.onResume();
        changeOverlayServiceState(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        changeOverlayServiceState(false);
    }

    @NonNull
    private View initViewBinding() {
        binding = ActivityClimateBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    private void restartOverlayServiceIfNeeded() {
        boolean isOverlayEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("floating_panel_enabled", false);

        if(isOverlayEnabled && !AdapterService.isAlive()) {
            ClimateOverlayService.start(this); // restart if service died by some reason
        }
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

    private void changeOverlayServiceState(boolean isActivityVisible) {
        if (!isActivityVisible
                && settingsPreferences.getBoolean("floating_panel_enabled", false)) {
            ClimateOverlayService.start(this);
        } else {
            ClimateOverlayService.stop(this);
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        AdapterService.AdapterBinder binder = (service instanceof AdapterService.AdapterBinder) ?
                (AdapterService.AdapterBinder) service : null;
        if(binder == null) {
            Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection failed");
            notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
            //TODO: show error
        } else {
            binder.registerListener(this);
            binding.adapterStatusButton.setImageResource(R.drawable.ic_connected);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died");
        notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
        binding.adapterStatusButton.setImageResource(R.drawable.ic_disconnected);
    }


    @Override
    public void onNewAdapterStateReceived(AdapterState newState) { // it runs in service's thread
        if(newState == null) {
            return;
        }
        binding.temp1.post(() -> binding.temp1.setText(String.valueOf(newState.getTempLeft())));
        binding.temp2.post(() -> binding.temp2.setText(String.valueOf(newState.getTempRight())));
        binding.acOnGlyph.post(() -> binding.acOnGlyph.setVisibility(newState.isAc() ? View.VISIBLE : View.GONE));
        binding.acOffGlyph.post(() -> binding.acOffGlyph.setVisibility(newState.isAc() ? View.GONE : View.VISIBLE));
        binding.autoGlyph.post(() -> binding.autoGlyph.setVisibility(newState.isAuto() ? View.VISIBLE : View.GONE));
        binding.fanSpeed.post(() -> binding.fanSpeed.setImageResource(newState.getFanLevel().getResourceId()));
        binding.fanDirection.post(() -> binding.fanDirection.setImageResource(newState.getFanDirection().getResourceId()));
    }
}