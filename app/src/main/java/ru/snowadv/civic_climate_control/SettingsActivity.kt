package ru.snowadv.civic_climate_control

import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import ru.snowadv.civic_climate_control.adapter.AdapterService
import ru.snowadv.civic_climate_control.databinding.SettingsActivityBinding
import ru.snowadv.civic_climate_control.flasher.FlasherActivity
import ru.snowadv.civic_climate_control.overlay.LayoutClimateOverlay
import ru.snowadv.civic_climate_control.overlay.NotificationClimateOverlay

private const val TAG = "SettingsFragment"
class SettingsActivity : AppCompatActivity() {
    private var binding: SettingsActivityBinding? = null
    private var settingsFragment: SettingsFragment? = null

    enum class Theme(val layoutId: Int, val stringNameId: Int) {
        HONDA_FULLSCREEN(
            R.layout.climate_fullscreen,
            R.string.fullscreen_honda_skin
        ),
        HONDA_OVERLAY(
            R.layout.climate_overlay, R.string.bottom_overlay_honda_skin
        ),
        HONDA_FULLSCREEN_TYPE_R(
            R.layout.climate_fullscreen_type_r, R.string.fullscren_type_r_honda_skin
        );

        fun toString(context: Context): String {
            return context.getString(stringNameId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            createSettingsFragment()
        }
        setContentView(initBinding())
        showBackButton()
        attachUsbBroadcastReceiver()
    }

    private fun showBackButton() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun createSettingsFragment() {
        settingsFragment = SettingsFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, settingsFragment!!)
            .commit()
    }

    private fun initBinding(): View {
        binding = SettingsActivityBinding.inflate(
            layoutInflater
        )
        return binding!!.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachUsbBroadcastReceiver(): UsbConnectionBroadcastReceiver {
        val usbConnectionBroadcastReceiver = UsbConnectionBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbConnectionBroadcastReceiver, filter)
        return usbConnectionBroadcastReceiver
    }

    inner class UsbConnectionBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            settingsFragment!!.updateDevicesList()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        private lateinit var preferenceScreen: PreferenceScreen
        private lateinit var connectedDevices: ListPreference
        private lateinit var selectedSkin: ListPreference
        private lateinit var adapterStatus: Preference
        private lateinit var floatingPanelSwitch: SwitchPreferenceCompat
        private lateinit var notificationsSwitch: SwitchPreferenceCompat
        private lateinit var floatingPanelDuration: SeekBarPreference
        private lateinit var floatingPanelHeight: SeekBarPreference
        private lateinit var flashAdapterButton: Preference
        private lateinit var companionDescription: Preference
        private lateinit var bluetoothCompanionDevices: ListPreference
        private lateinit var companionServiceSwitch: SwitchPreferenceCompat
        private lateinit var askForOverlayPermission: ActivityResultLauncher<String?>
        private fun initFields() {
            preferenceScreen = getPreferenceScreen()
            connectedDevices = findPreference("adapter_name")!!
            adapterStatus = findPreference("adapter_status")!!
            floatingPanelSwitch = findPreference("floating_panel_enabled")!!
            notificationsSwitch = findPreference("notifications_enabled")!!
            floatingPanelDuration = findPreference("floating_panel_duration")!!
            floatingPanelHeight = findPreference("overlay_height")!!
            flashAdapterButton = findPreference("flash_adapter")!!
            selectedSkin = findPreference("selected_skin")!!
            companionDescription = findPreference("companion_description")!!
            bluetoothCompanionDevices = findPreference("companion_mac_address")!!
            companionServiceSwitch = findPreference("companion_enabled")!!
            askForOverlayPermission = createOverlayPermissionRequest()
        }

        private fun createOverlayPermissionRequest(): ActivityResultLauncher<String?> {
            return registerForActivityResult(object :
                ActivityResultContract<String?, Boolean?>() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                override fun createIntent(context: Context, input: String?): Intent {
                    return Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
                    return Settings.canDrawOverlays(context)
                }
            }) { result: Boolean? -> processIntentResult(result ?: false, android.Manifest.permission.SYSTEM_ALERT_WINDOW) }
        }


        private fun processIntentResult(result: Boolean, permission: String) {
            when(permission) {
                android.Manifest.permission.SYSTEM_ALERT_WINDOW -> if (result) {
                    LayoutClimateOverlay.start(requireContext())
                } else {
                    floatingPanelSwitch.isChecked = false
                    Toast.makeText(requireContext(), R.string.unable_to_get_overlay_permission,
                        Toast.LENGTH_LONG).show()
                }
            }
        }


        private fun getPermissionAndStartOverlayService() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(context)
            ) {
                explainOverlayPermission()
            } else {
                LayoutClimateOverlay.start(requireContext())
            }
        }
        @RequiresApi(Build.VERSION_CODES.M)
        private fun explainOverlayPermission() {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.need_permission)
                .setMessage(R.string.please_allow_overlay)
                .setPositiveButton(R.string.ok) { dialog: DialogInterface?, which: Int ->
                    askForOverlayPermission.launch("")
                }
                .setNegativeButton(R.string.cancel) { dialog: DialogInterface?, which: Int ->
                    floatingPanelSwitch.isChecked = false
                }
                .show()
        }



        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            initFields()
            initListeners()
            updateSkinsList()
        }

        override fun onResume() {
            super.onResume()
            updateDevicesList()
            //updateBluetoothDevicesList()
        }

        private fun initListeners() {
            floatingPanelSwitch.onPreferenceChangeListener = this
            notificationsSwitch.onPreferenceChangeListener = this
            connectedDevices.onPreferenceChangeListener = this
            floatingPanelDuration.onPreferenceChangeListener = this
            floatingPanelHeight.onPreferenceChangeListener = this
            flashAdapterButton.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? -> askAndOpenFlashActivity() }
            selectedSkin.onPreferenceChangeListener = this
            companionDescription.onPreferenceChangeListener = this
            bluetoothCompanionDevices.onPreferenceChangeListener = this
            companionServiceSwitch.onPreferenceChangeListener = this
        }

        private fun askAndOpenFlashActivity(): Boolean {
            activity?.let { activity ->
                val builder = AlertDialog.Builder(
                    activity.applicationContext
                )
                if (!isAdapterConnected) {
                    builder.setMessage(R.string.please_choose_adapter)
                        .setPositiveButton(android.R.string.ok, null)
                } else {
                    builder.setTitle(R.string.uno_only).setMessage(R.string.uno_only_are_you_sure)
                        .setPositiveButton(android.R.string.yes) { dialog: DialogInterface?, which: Int -> openFlashActivity() }
                        .setNegativeButton(android.R.string.no, null)
                }
                return builder.show() != null
            } ?: return false
        }

        private fun openFlashActivity() {
            if (activity == null) return
            val serializedDevice = SerializableUsbDevice.fromJson(
                connectedDevices.value
            )
            AdapterService.getAccessToDevice(activity, serializedDevice) { usbDevice: UsbDevice? ->
                Log.d(TAG, "openFlashActivity: opening")
                val intent = Intent(activity, FlasherActivity::class.java)
                intent.putExtra("adapter", usbDevice)
                activity?.startActivity(intent)?.let { true } ?: false
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        fun updateDevicesList() {
            val devicesList = DevicesManager.INSTANCE.getUsbDevices(context)
            connectedDevices.entries =
                devicesList.keys.toTypedArray()
            connectedDevices.entryValues =
                devicesList.values.toTypedArray()
            setDeviceConnectionStatus(isAdapterConnected(devicesList))
        }


        fun updateBluetoothDevicesList() {
            val devices = DevicesManager.INSTANCE.getBluetoothDevices(requireContext()) {
                /*Toast.makeText(
                    requireContext(),
                    "No bluetooth permission",
                    Toast.LENGTH_LONG
                ).show() // TODO: request bt permission on api >=31*/
            }
            bluetoothCompanionDevices.entries =
                devices.keys.toTypedArray()
            bluetoothCompanionDevices.entryValues =
                devices.values.toTypedArray()
        }

        private fun updateSkinsList() {
            selectedSkin.entries = Theme.values().map { it.toString(requireContext()) }.toTypedArray()
            selectedSkin.entryValues = Theme.values().map { it.toString() }.toTypedArray()
        }

        private fun isAdapterConnected(devicesList: Map<String, String>): Boolean {
            return devicesList.containsValue(connectedDevices!!.value)
        }

        private val isAdapterConnected: Boolean
            private get() {
                val devicesList = DevicesManager.INSTANCE.getUsbDevices(context)
                return isAdapterConnected(devicesList)
            }

        private fun setDeviceConnectionStatus(isConnected: Boolean) {
            adapterStatus!!.setSummary(if (isConnected) R.string.connected else R.string.disconnected)
        }

        private fun changeFloatingPanelState(newState: Boolean) {
            if (newState) {
                getPermissionAndStartOverlayService()
            } else {
                LayoutClimateOverlay.stop(requireContext())
            }
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            Log.d(TAG, "onPreferenceChange: " + preference.key)
            when (preference.key) {
                "adapter_name" -> {
                    updateDevicesList()
                    Log.d(TAG, "onPreferenceChange: restarting overlay")
                    changeFloatingPanelState(false) // Restart overlay if device changed
                    changeFloatingPanelState(floatingPanelSwitch.isChecked)
                }

                "floating_panel_duration", "overlay_height", "selected_skin" -> {
                    Log.d(TAG, "onPreferenceChange: restarting overlay")
                    changeFloatingPanelState(false)
                    changeFloatingPanelState(floatingPanelSwitch.isChecked)
                }

                "floating_panel_enabled" -> changeFloatingPanelState(newValue as Boolean)
                "notifications_enabled" -> NotificationClimateOverlay.changeServiceState(
                    (newValue as Boolean),
                    context
                )

                "companion_enabled" -> {
                    if (!DevicesManager.INSTANCE.checkBtPermission(requireContext()))
                        companionServiceSwitch.isChecked = false
                }
            }
            return true
        }

    }
}