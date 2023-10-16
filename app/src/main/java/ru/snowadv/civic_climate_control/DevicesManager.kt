package ru.snowadv.civic_climate_control

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.util.stream.Collectors

enum class DevicesManager {
    INSTANCE;

    private val TAG = "DevicesManager"
    fun getUsbDevices(context: Context?): Map<String, String> = getUsbDevices(getUsbManager(context))

    fun getUsbDevices(usbManager: UsbManager?): Map<String, String> {
        return usbManager?.deviceList?.values?.stream()
            ?.map { usbDevice: UsbDevice? -> SerializableUsbDevice(usbDevice) }
            ?.collect(
                Collectors.toMap(
                    { obj: SerializableUsbDevice -> obj.toString() },
                    { obj: SerializableUsbDevice -> obj.toJson() })
            ) ?: HashMap()
    }
    fun checkBtPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    fun getBluetoothDevices(context: Context, noPermissionCallback: () -> Unit): Map<String, String> {
        if (checkBtPermission(context)) {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                ?.bondedDevices?.associateBy ({it.address}, {"${it.name} [${it.address}]"})?.let { return it }
        } else {
            noPermissionCallback()
        }
        return HashMap()
    }


    private fun getUsbManager(context: Context?): UsbManager? {
        return context?.getSystemService(Context.USB_SERVICE) as UsbManager?
    }

}