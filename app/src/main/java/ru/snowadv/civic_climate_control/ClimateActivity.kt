package ru.snowadv.civic_climate_control

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.gson.JsonSyntaxException
import ru.snowadv.civic_climate_control.adapter.AdapterService
import ru.snowadv.civic_climate_control.adapter.AdapterService.AdapterBinder
import ru.snowadv.civic_climate_control.adapter.AdapterService.OnNewStateReceivedListener
import ru.snowadv.civic_climate_control.adapter.AdapterState
import ru.snowadv.civic_climate_control.databinding.ActivityClimateBinding
import ru.snowadv.civic_climate_control.overlay.LayoutClimateOverlay
import ru.snowadv.civic_climate_control.ui.AppListDialog
import java.util.Objects

class ClimateActivity : AppCompatActivity(), ServiceConnection, OnNewStateReceivedListener {
    private val TAG = "ClimateActivity"
    private val DONT_SHOW_NEW_DEVICE_DIALOG = true
    private var binding: ActivityClimateBinding? = null
    private var settingsPreferences: SharedPreferences? = null
    private var notifierUtility: NotifierUtility? = null
    private var serviceConnectionAlive = false
    private var usbConnectedBroadcastReceiver: UsbConnectedBroadcastReceiver? = null
    private var rootView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notifierUtility = NotifierUtility(this)
        hideTitleAndNotificationBars()
        rootView = initViewBinding()
        setContentView(rootView)
        initFields()
        initInterfaceListeners()
        restartOverlayServiceIfNeeded()
        val intent = intent
        if (intent.action == "android.hardware.usb.action.USB_DEVICE_ATTACHED") {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            Toast.makeText(
                this, String.format(
                    getString(R.string.set_default_device),
                    device!!.productName
                ), Toast.LENGTH_LONG
            ).show()
            val serializableUsbDevice = SerializableUsbDevice(device)
            saveDeviceToSettings(serializableUsbDevice)
            initAdapterService(serializableUsbDevice)
        } else {
            initAdapterService()
        }
        usbConnectedBroadcastReceiver = UsbConnectedBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(usbConnectedBroadcastReceiver, filter)
    }

    private fun saveDeviceToSettings(serializableUsbDevice: SerializableUsbDevice) {
        val edit = PreferenceManager.getDefaultSharedPreferences(this).edit()
        edit.putString("adapter_name", serializableUsbDevice.toJson())
        edit.apply()
    }

    private fun initAdapterService() {
        val prefManager = PreferenceManager.getDefaultSharedPreferences(this)
        try {
            val adapterDevice = getStoredSerializableUsbDevice(prefManager)
            initAdapterService(adapterDevice)
        } catch (exception: JsonSyntaxException) {
            prefManager.edit().remove("adapter_name").apply()
            initAdapterService(null)
        }
    }

    private fun getStoredSerializableUsbDevice(prefManager: SharedPreferences): SerializableUsbDevice? {
        val adapterDeviceJson = prefManager.getString("adapter_name", null)
        return SerializableUsbDevice.fromJson(adapterDeviceJson)
    }

    private val storedSerializableUsbDevice: SerializableUsbDevice?
        private get() = getStoredSerializableUsbDevice(
            PreferenceManager.getDefaultSharedPreferences(
                this
            )
        )

    private fun initAdapterService(adapterDevice: SerializableUsbDevice?) {
        adapterDevice?.let { AdapterService.getAccessAndBindService(this, it, this) }
            ?: Toast.makeText(this,
                R.string.set_device_please, Toast.LENGTH_LONG).show()
    }

    public override fun onResume() {
        super.onResume()
        if (!serviceConnectionAlive) {
            initAdapterService()
        }
        changeOverlayServiceState(true)
    }

    override fun onPause() {
        super.onPause()
        changeOverlayServiceState(false)
    }

    private fun initViewBinding(): View {
        binding = ActivityClimateBinding.inflate(
            layoutInflater
        )
        return binding!!.root
    }

    private fun restartOverlayServiceIfNeeded() {
        val climateAutoStart = ClimateAutoStart()
        climateAutoStart.onReceive(this, this.intent)
    }

    private fun initFields() {
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onDestroy() {
        if (usbConnectedBroadcastReceiver != null) {
            unregisterReceiver(usbConnectedBroadcastReceiver)
        }
        super.onDestroy()
    }

    private fun initInterfaceListeners() {
        binding?.settingsButton?.setOnClickListener { view: View? -> openSettingsActivity() }
        binding?.adapterStatusButton?.setOnClickListener { view: View? ->
            restartServiceDialog(
                serviceConnectionAlive
            )
        }
        binding?.appsButton?.setOnClickListener {
            val appListDialog = AppListDialog(this)
            appListDialog.show()
        }
    }

    private fun restartServiceDialog(isAdapterConnectionAlive: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(
            if (isAdapterConnectionAlive) getString(R.string.adapter_connected) else getString(
                R.string.adapter_not_connected
            )
        )
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, which: Int -> }
        if (!isAdapterConnectionAlive) {
            builder.setPositiveButton(R.string.restart_service_button) { dialog: DialogInterface?, which: Int -> initAdapterService() }
        }
        builder.create().show()
    }

    private fun hideTitleAndNotificationBars() {
        supportActionBar?.hide()
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun openSettingsActivity() {
        val settingsIntent = Intent(this@ClimateActivity, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    private fun changeOverlayServiceState(isActivityVisible: Boolean) {
        val isOverlayEnabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("floating_panel_enabled", false)
        if (!isOverlayEnabled) return
        if (!isActivityVisible) {
            LayoutClimateOverlay.start(this) // this will restart service if needed
        }
        LayoutClimateOverlay.setClimateActivityIsVisible(isActivityVisible)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = if (service is AdapterBinder) service else null
        binder?.let {
            binder.registerListener(this)
            binding?.adapterStatusButton?.setImageResource(R.drawable.ic_connected)
            serviceConnectionAlive = true
        } ?: run {
            Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection failed")
            notifierUtility?.reportErrorInNotification(this, R.string.adapter_not_connected)
            Toast.makeText(this, getString(R.string.connection_failed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died")
        notifierUtility?.reportErrorInNotification(this, R.string.adapter_not_connected)
        binding?.adapterStatusButton?.setImageResource(R.drawable.ic_disconnected)
        serviceConnectionAlive = false
    }

    override fun onBindingDied(name: ComponentName) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection's binding died")
        notifierUtility!!.reportErrorInNotification(this, R.string.adapter_not_connected)
        binding!!.adapterStatusButton.setImageResource(R.drawable.ic_disconnected)
        serviceConnectionAlive = false
    }

    override fun onNewAdapterStateReceived(newState: AdapterState) { // it runs in service's thread
        binding?.let { binding ->
            with(binding) {
                binding.root.post {
                    temp1Background.visibility =
                        if (newState.tempLeftVisibility) View.VISIBLE else View.GONE

                    temp1.visibility =
                        if (newState.tempLeftVisibility) View.VISIBLE else View.GONE
                    temp1.text = newState.tempLeftString

                    temp2Background.visibility =
                        if (newState.tempRightVisibility) View.VISIBLE else View.GONE

                    temp2.visibility =
                        if (newState.tempRightVisibility) View.VISIBLE else View.GONE
                    temp2.text = newState.tempRightString

                    acOnGlyph.alpha = if (newState.acState == AdapterState.ACState.ON) 1.0f else 0.0f

                    acOffGlyph.alpha = if (newState.acState == AdapterState.ACState.OFF) 1.0f else 0.0f

                    autoGlyph.visibility = if (newState.acState == AdapterState.ACState.ON) View.VISIBLE else View.GONE

                    fanSpeed.setImageResource(newState.fanLevel.resourceId)

                    fanDirection.setImageResource(newState.fanDirection.resourceId)
                }
            }
        }
    }

    override fun onAdapterDisconnected() {
        try {
            unbindService(this)
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "onAdapterDisconnected: tried to unbind service, but it is already dead")
        }
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died")
        notifierUtility?.reportErrorInNotification(this, R.string.adapter_not_connected)
        binding?.adapterStatusButton?.setImageResource(R.drawable.ic_disconnected)
        serviceConnectionAlive = false
    }

    inner class UsbConnectedBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device =
                    SerializableUsbDevice(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))
                if (device == storedSerializableUsbDevice) {
                    initAdapterService(device)
                } else {
                    offerToSetNewUsbDevice(device)
                }
            }
        }
    }

    private fun offerToSetNewUsbDevice(device: SerializableUsbDevice) {
        if (DONT_SHOW_NEW_DEVICE_DIALOG) {
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.device_detected)
            .setMessage(
                String.format(
                    getString(R.string.set_new_device_as_default),
                    device.productName
                )
            )
            .setPositiveButton(android.R.string.yes) { dialog: DialogInterface?, which: Int ->
                if (serviceConnectionAlive) {
                    val myService = Intent(
                        this@ClimateActivity,
                        AdapterService::class.java
                    )
                    stopService(myService)
                }
                saveDeviceToSettings(device)
                initAdapterService(device)
            }
            .setNegativeButton(android.R.string.no) { dialog: DialogInterface?, which: Int -> }
        builder.create().show()
    }
}