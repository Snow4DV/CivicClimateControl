package ru.snowadv.civic_climate_control;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.snowadv.civic_climate_control.databinding.SettingsActivityBinding;

public class SettingsActivity extends AppCompatActivity {

    private SettingsActivityBinding binding;
    private SettingsFragment settingsFragment;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            createSettingsFragment();
        }

        setContentView(initBinding());
        showBackButton();

        attachUsbBroadcastReceiver();
    }



    private void showBackButton() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void createSettingsFragment() {
        settingsFragment = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
    }





    private View initBinding() {
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private UsbConnectionBroadcastReceiver attachUsbBroadcastReceiver() {
        UsbConnectionBroadcastReceiver usbConnectionBroadcastReceiver
                = new UsbConnectionBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbConnectionBroadcastReceiver, filter);
        return usbConnectionBroadcastReceiver;
    }

    public class UsbConnectionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            settingsFragment.updateDevicesList();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        private static final String TAG = "SettingsFragment";
        private PreferenceScreen preferenceScreen;
        private ListPreference connectedDevices;
        private Preference adapterStatus;
        private SwitchPreferenceCompat floatingPanelSwitch;
        private SeekBarPreference floatingPanelDuration;

        private ActivityResultLauncher<String> askPermission;

        private void initFields() {
            preferenceScreen = getPreferenceScreen();
            connectedDevices = findPreference("adapter_name");
            adapterStatus = findPreference("adapter_status");
            floatingPanelSwitch = findPreference("floating_panel_enabled");
            floatingPanelDuration = findPreference("floating_panel_duration");
            askPermission = getAskPermission();

        }


        @NonNull
        private ActivityResultLauncher<String> getAskPermission() {
            return registerForActivityResult(new ActivityResultContract<String, Boolean>() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, String input) {
                    return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + context.getPackageName()));
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public Boolean parseResult(int resultCode, @Nullable Intent intent) {
                    return Settings.canDrawOverlays(getContext());
                }
            }, (ActivityResultCallback<Boolean>) this::processIntentResult);
        }

        private void processIntentResult(Boolean result) {
            if(result) {
                ClimateOverlayService.start(requireContext());
            } else {
                floatingPanelSwitch.setChecked(false);
            }
        }

        private void getPermissionAndStartOverlayService() {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.canDrawOverlays(getContext())) {
                explainOverlayPermission();
            } else {
                ClimateOverlayService.start(requireContext());
            }
        }

        private void explainOverlayPermission() {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.need_permission)
                    .setMessage(R.string.please_allow_overlay)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        askPermission.launch("");
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        floatingPanelSwitch.setChecked(false);
                    })
                    .show();
        }


        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            initFields();
            initListeners();

            updateDevicesList();
        }

        private void initListeners() {
            floatingPanelSwitch.setOnPreferenceChangeListener(this);
            connectedDevices.setOnPreferenceChangeListener(this);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        public void updateDevicesList() {
            Map<String, String> devicesList = DevicesManager.INSTANCE.getDevicesList(getContext());
            connectedDevices.setEntries(devicesList.keySet().toArray(new CharSequence[]{}));
            connectedDevices.setEntryValues(devicesList.values().toArray(new CharSequence[]{}));
            setDeviceConnectionStatus(devicesList.containsValue(connectedDevices.getValue()));
        }

        private void setDeviceConnectionStatus(boolean isConnected) {
            adapterStatus.setSummary(isConnected ? R.string.connected : R.string.disconnected);
        }

        private void changeFloatingPanelState(Boolean newState) {
            if(newState) {
                getPermissionAndStartOverlayService();
            } else {
                ClimateOverlayService.stop(requireContext());
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch(preference.getKey()) {
                case "adapter_name":
                    updateDevicesList();
                    changeFloatingPanelState(false); // Restart overlay if device changed
                case "floating_panel_enabled":
                    changeFloatingPanelState((Boolean) newValue);
                    break;
            }
            return true;
        }
    }
}