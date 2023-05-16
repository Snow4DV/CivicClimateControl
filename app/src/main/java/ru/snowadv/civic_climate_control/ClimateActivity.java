package ru.snowadv.civic_climate_control;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.JsonSyntaxException;

import java.util.Objects;

import ru.snowadv.civic_climate_control.Adapter.AdapterService;
import ru.snowadv.civic_climate_control.Adapter.AdapterState;
import ru.snowadv.civic_climate_control.databinding.ActivityClimateBinding;

public class ClimateActivity extends AppCompatActivity implements ServiceConnection, AdapterService.OnNewStateReceivedListener {


    private static final String TAG = "ClimateActivity";
    private static final boolean DONT_SHOW_NEW_DEVICE_DIALOG = true;
    private ActivityClimateBinding binding;
    private SharedPreferences settingsPreferences;
    private NotifierUtility notifierUtility;
    private boolean serviceConnectionAlive = false;
    private UsbConnectedBroadcastReceiver usbConnectedBroadcastReceiver;
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

        Intent intent = getIntent();
        if(intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Toast.makeText(this, String.format(getString(R.string.set_default_device),
                    device.getProductName()), Toast.LENGTH_LONG).show();
            SerializableUsbDevice serializableUsbDevice = new SerializableUsbDevice(device);
            saveDeviceToSettings(serializableUsbDevice);
            initAdapterService(serializableUsbDevice);
        } else {
            initAdapterService();
        }

        usbConnectedBroadcastReceiver = new UsbConnectedBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbConnectedBroadcastReceiver, filter);
    }

    private void saveDeviceToSettings(SerializableUsbDevice serializableUsbDevice) {
        SharedPreferences.Editor edit =
                PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString("adapter_name", serializableUsbDevice.toJson());
        edit.apply();
    }


    private void initAdapterService() {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            SerializableUsbDevice adapterDevice = getStoredSerializableUsbDevice(prefManager);
            initAdapterService(adapterDevice);
        } catch(JsonSyntaxException exception) {
            prefManager.edit().remove("adapter_name").apply();
            initAdapterService(null);
        }
    }

    private SerializableUsbDevice getStoredSerializableUsbDevice(SharedPreferences prefManager) {
        String adapterDeviceJson = prefManager.getString("adapter_name", null);
        return SerializableUsbDevice.fromJson(adapterDeviceJson);
    }


    private SerializableUsbDevice getStoredSerializableUsbDevice() {
        return getStoredSerializableUsbDevice(PreferenceManager.getDefaultSharedPreferences(this));
    }
    private void initAdapterService(SerializableUsbDevice adapterDevice) {
        if(adapterDevice == null) {
            Toast.makeText(this, R.string.set_device_please, Toast.LENGTH_LONG).show();
        } else {
            AdapterService.getAccessAndBindService(this, adapterDevice, this);
        }
    }

    public void onResume() {
        super.onResume();
        if(!serviceConnectionAlive) {
            initAdapterService();
        }
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


    @Override
    protected void onDestroy() {
        if(usbConnectedBroadcastReceiver != null) {
            this.unregisterReceiver(usbConnectedBroadcastReceiver);
        }
        super.onDestroy();
    }

    private void initInterfaceListeners() {
        binding.settingsButton.setOnClickListener(view -> openSettingsActivity());
        binding.adapterStatusButton.setOnClickListener(view ->
                restartServiceDialog(serviceConnectionAlive));
    }

    private void restartServiceDialog(boolean isAdapterConnectionAlive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(isAdapterConnectionAlive ? getString(R.string.adapter_connected) :
                        getString(R.string.adapter_not_connected))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {});
        if(!isAdapterConnectionAlive) {
            builder.setPositiveButton(R.string.restart_service_button, (dialog, which) -> {
                initAdapterService();
            });
        }
        builder.create().show();
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
        boolean isOverlayEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("floating_panel_enabled", false);
        if(!isOverlayEnabled) return;

        if(!isActivityVisible) {
            ClimateOverlayService.start(this); // this will restart service if needed
        }
        ClimateOverlayService.setClimateActivityIsVisible(isActivityVisible);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        AdapterService.AdapterBinder binder = (service instanceof AdapterService.AdapterBinder) ?
                (AdapterService.AdapterBinder) service : null;
        if(binder == null) {
            Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection failed");
            notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
            Toast.makeText(this, getString(R.string.connection_failed), Toast.LENGTH_SHORT)
                    .show();
        } else {
            binder.registerListener(this);
            binding.adapterStatusButton.setImageResource(R.drawable.ic_connected);
            serviceConnectionAlive = true;
        }
    }



    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died");
        notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
        binding.adapterStatusButton.setImageResource(R.drawable.ic_disconnected);
        serviceConnectionAlive = false;
    }

    @Override
    public void onBindingDied(ComponentName name) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection's binding died");
        notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
        binding.adapterStatusButton.setImageResource(R.drawable.ic_disconnected);
        serviceConnectionAlive = false;
    }

    @Override
    public void onNewAdapterStateReceived(AdapterState newState) { // it runs in service's thread
        if(newState == null) {
            return;
        }

        binding.temp1Background.post(() -> binding.temp1Background.setVisibility(
                newState.isTempLeftVisible() ? View.VISIBLE : View.GONE));
        binding.temp1.post(() -> binding.temp1.setVisibility(newState.isTempLeftVisible() ?
                View.VISIBLE : View.GONE));
        binding.temp1.post(() -> binding.temp1.setText(newState.getTempLeftString()));

        binding.temp2Background.post(() -> binding.temp2Background.setVisibility(
                newState.isTempRightVisible() ? View.VISIBLE : View.GONE));
        binding.temp2.post(() -> binding.temp2.setVisibility(newState.isTempRightVisible() ?
                View.VISIBLE : View.GONE));
        binding.temp2.post(() -> binding.temp2.setText(newState.getTempRightString()));

        binding.acOnGlyph.post(() -> binding.acOnGlyph.setAlpha(newState.getAcState() ==
                AdapterState.ACState.ON ? 1.0f : 0.0f));
        binding.acOffGlyph.post(() -> binding.acOffGlyph.setAlpha(newState.getAcState()
                == AdapterState.ACState.OFF ? 1.0f : 0.0f));

        binding.autoGlyph.post(() -> binding.autoGlyph.setVisibility(newState.isAuto() ?
                View.VISIBLE : View.GONE));
        binding.fanSpeed.post(() -> binding.fanSpeed.setImageResource(
                newState.getFanLevel().getResourceId()));
        binding.fanDirection.post(() -> binding.fanDirection.setImageResource(
                newState.getFanDirection().getResourceId()));
    }

    @Override
    public void onAdapterDisconnected() {
        try {
            unbindService(this);
        } catch(IllegalArgumentException exception) {
            Log.e(TAG, "onAdapterDisconnected: tried to unbind service, but it is already dead");
        }
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died");
        notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
        binding.adapterStatusButton.setImageResource(R.drawable.ic_disconnected);
        serviceConnectionAlive = false;
    }

    public class UsbConnectedBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                SerializableUsbDevice device = new
                        SerializableUsbDevice(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
                if(device.equals(getStoredSerializableUsbDevice())) {
                    initAdapterService(device);
                } else {
                    offerToSetNewUsbDevice(device);
                }
            }
        }
    }

    private void offerToSetNewUsbDevice(SerializableUsbDevice device) {
        if(DONT_SHOW_NEW_DEVICE_DIALOG) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.device_detected)
                .setMessage(String.format(getString(R.string.set_new_device_as_default),
                        device.getProductName()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    if(serviceConnectionAlive) {
                        Intent myService = new Intent(ClimateActivity.this,
                                AdapterService.class);
                        stopService(myService);
                    }
                    saveDeviceToSettings(device);
                    initAdapterService(device);
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {});
        builder.create().show();
    }
}