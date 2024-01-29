package ru.snowadv.civic_climate_control

import android.app.ActionBar.LayoutParams
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
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


abstract class AbstractServiceConnectedActivity : AppCompatActivity(), ServiceConnection, OnNewStateReceivedListener {
    private val DONT_SHOW_NEW_DEVICE_DIALOG = true
    protected lateinit var settingsPreferences: SharedPreferences
    protected var serviceConnectionAlive = false
    protected abstract val TAG: String
    private var usbConnectedBroadcastReceiver: UsbConnectedBroadcastReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSettingsPrefs()
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


    protected fun initAdapterService() {

        try {
            val adapterDevice = getStoredSerializableUsbDevice(settingsPreferences)
            initAdapterService(adapterDevice)
        } catch (exception: JsonSyntaxException) {
            settingsPreferences.edit().remove("adapter_name").apply()
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

    protected fun initAdapterService(adapterDevice: SerializableUsbDevice?) {
        adapterDevice?.let { AdapterService.getAccessAndBindService(this, it, this) }
            ?: Toast.makeText(this,
                R.string.set_device_please, Toast.LENGTH_LONG).show()
    }

    public override fun onResume() {
        super.onResume()
        if (!serviceConnectionAlive) {
            initAdapterService()
        }
    }

    override fun onPause() {
        super.onPause()
    }


    private fun initSettingsPrefs() {
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onDestroy() {
        if (usbConnectedBroadcastReceiver != null) {
            unregisterReceiver(usbConnectedBroadcastReceiver)
        }
        super.onDestroy()
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = if (service is AdapterBinder) service else null
        binder?.let {
            binder.registerListener(this)
            serviceConnectionAlive = true
        } ?: run {
            Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection failed")
            Toast.makeText(this, getString(R.string.connection_failed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died")
        serviceConnectionAlive = false
    }

    override fun onBindingDied(name: ComponentName) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection's binding died")
        serviceConnectionAlive = false
    }


    override fun onAdapterDisconnected() {
        try {
            unbindService(this)
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "onAdapterDisconnected: tried to unbind service, but it is already dead")
        }
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died")
        serviceConnectionAlive = false
    }

    protected fun saveDeviceToSettings(serializableUsbDevice: SerializableUsbDevice) {
        val edit = PreferenceManager.getDefaultSharedPreferences(this).edit()
        edit.putString("adapter_name", serializableUsbDevice.toJson())
        edit.apply()
    }

    private inner class UsbConnectedBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device =
                    SerializableUsbDevice(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))
                if (device == storedSerializableUsbDevice) {
                    initAdapterService(device)
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
                        this@AbstractServiceConnectedActivity,
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