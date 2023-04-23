package ru.snowadv.civic_climate_control;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public enum DevicesManager {
    INSTANCE;

    private static final String TAG = "DevicesManager";

    public Map<String, String> getDevicesList(Context context) {
        return getDevicesList(getUsbManager(context));
    }

    public Map<String, String> getDevicesList(UsbManager usbManager) {
        if(usbManager == null) {
            return new HashMap<>();
        }
        return usbManager.getDeviceList().values().stream()
                .map(SerializableUsbDevice::new)
                .collect(Collectors.toMap(SerializableUsbDevice::toString,
                        SerializableUsbDevice::toJson));
    }



    private UsbManager getUsbManager(Context context) {
        return context == null ? null : (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }



}
