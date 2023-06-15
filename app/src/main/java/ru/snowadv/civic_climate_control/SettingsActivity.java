package ru.snowadv.civic_climate_control;

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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.snowadv.civic_climate_control.Adapter.AdapterService;
import ru.snowadv.civic_climate_control.databinding.SettingsActivityBinding;
import ru.snowadv.civic_climate_control.flasher.FlasherActivity;

public class SettingsActivity extends AppCompatActivity {

    private SettingsActivityBinding binding;
    private SettingsFragment settingsFragment;

    public enum Theme {
        HONDA_FULLSCREEN(R.layout.climate_fullscreen, R.string.fullscreen_honda_skin),
        HONDA_OVERLAY(R.layout.climate_overlay, R.string.bottom_overlay_honda_skin),
        HONDA_FULLSCREEN_TYPE_R(R.layout.climate_fullscreen_type_r, R.string.fullscren_type_r_honda_skin);
        private int layoutId;
        private int stringNameId;

        Theme(int layoutId, int stringNameId) {
            this.layoutId = layoutId;
            this.stringNameId = stringNameId;
        }

        public int getLayoutId() {
            return layoutId;
        }

        public int getStringNameId() {
            return stringNameId;
        }

        public static String[] stringKeys() {
            return Arrays.stream(values()).map(Objects::toString).toArray(String[]::new);
        }
        public static String[] stringValues(Context context) {
            return Arrays.stream(Theme.values()).map((value) -> value.toString(context))
                    .toArray(String[]::new);
        }

       @NonNull
        public String toString(Context context) {
            return context.getString(stringNameId);
        }
    }

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
        private ListPreference selectedSkin;
        private Preference adapterStatus;
        private SwitchPreferenceCompat floatingPanelSwitch;
        private SeekBarPreference floatingPanelDuration;
        private SeekBarPreference floatingPanelHeight;
        private Preference flashAdapterButton;

        private ActivityResultLauncher<String> askPermission;

        private void initFields() {
            preferenceScreen = getPreferenceScreen();
            connectedDevices = findPreference("adapter_name");
            adapterStatus = findPreference("adapter_status");
            floatingPanelSwitch = findPreference("floating_panel_enabled");
            floatingPanelDuration = findPreference("floating_panel_duration");
            floatingPanelHeight = findPreference("overlay_height");
            flashAdapterButton = findPreference("flash_adapter");
            selectedSkin = findPreference("selected_skin");
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
            }, this::processIntentResult);
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
            updateSkinsList();
        }

        private void initListeners() {
            floatingPanelSwitch.setOnPreferenceChangeListener(this);
            connectedDevices.setOnPreferenceChangeListener(this);
            floatingPanelDuration.setOnPreferenceChangeListener(this);
            floatingPanelHeight.setOnPreferenceChangeListener(this);
            flashAdapterButton.setOnPreferenceClickListener(preference -> askAndOpenFlashActivity());
            selectedSkin.setOnPreferenceChangeListener(this);
        }

        private boolean askAndOpenFlashActivity() {
            if(getActivity() == null) return false;



            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            if(!isAdapterConnected()) {
                builder.setMessage(R.string.please_choose_adapter)
                        .setPositiveButton(android.R.string.ok, null);
            } else {
                builder.setTitle(R.string.uno_only).setMessage(R.string.uno_only_are_you_sure)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> openFlashActivity())
                        .setNegativeButton(android.R.string.no, null);
            }

            return builder.show() != null;
        }
        private void openFlashActivity() {
            if(getActivity() == null) return;
            SerializableUsbDevice serializedDevice =
                    SerializableUsbDevice.fromJson(connectedDevices.getValue());

            AdapterService.getAccessToDevice(getActivity(), serializedDevice, usbDevice -> {
                Log.d(TAG, "openFlashActivity: opening");
                Intent intent = new Intent(getActivity(), FlasherActivity.class);
                intent.putExtra("adapter", usbDevice);
                if(getActivity() != null) {
                    getActivity().startActivity(intent);
                }
                return true;
            });


        }
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        private void updateDevicesList() {
            Map<String, String> devicesList = DevicesManager.INSTANCE.getDevicesList(getContext());
            connectedDevices.setEntries(devicesList.keySet().toArray(new CharSequence[]{}));
            connectedDevices.setEntryValues(devicesList.values().toArray(new CharSequence[]{}));
            setDeviceConnectionStatus(isAdapterConnected(devicesList));
        }


        private void updateSkinsList() {
            selectedSkin.setEntries(Theme.stringValues(requireContext()));
            selectedSkin.setEntryValues(Theme.stringKeys());
        }

        private boolean isAdapterConnected(Map<String, String> devicesList) {
            return devicesList.containsValue(connectedDevices.getValue());
        }


        private boolean isAdapterConnected() {
            Map<String, String> devicesList = DevicesManager.INSTANCE.getDevicesList(getContext());
            return isAdapterConnected(devicesList);
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
            Log.d(TAG, "onPreferenceChange: " + preference.getKey());
            switch(preference.getKey()) {
                case "adapter_name":
                    updateDevicesList();
                case "floating_panel_duration":
                case "overlay_height":
                case "selected_skin":
                    Log.d(TAG, "onPreferenceChange: restarting overlay");
                    changeFloatingPanelState(false); // Restart overlay if device changed
                    changeFloatingPanelState(floatingPanelSwitch.isChecked());
                    break;
                case "floating_panel_enabled":
                    changeFloatingPanelState((Boolean) newValue);
                    break;
            }
            return true;
        }
    }
}