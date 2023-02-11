package ru.snowadv.civic_climate_control;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.Serializable;

public class SerializableUsbDevice {
    private int vendorId;
    private int deviceId;
    private String productName;
    private static final Gson gson = new Gson();


    public SerializableUsbDevice(int vendorId, int deviceId, String productName) {
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.productName = productName;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public String getProductName() {
        return productName;
    }

    public static Gson getGson() {
        return gson;
    }

    public SerializableUsbDevice(UsbDevice usbDevice) {
        this.vendorId = usbDevice.getVendorId();
        this.deviceId = usbDevice.getDeviceId();
        this.productName = usbDevice.getProductName();
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static SerializableUsbDevice fromJson(String json) {
        return gson.fromJson(json, SerializableUsbDevice.class);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s. VID: %s, PID: %s", productName, vendorId, deviceId);
    }

    public boolean isDescribingUsbDevice(UsbDevice device) {
        return vendorId == device.getVendorId() && deviceId == device.getProductId();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof SerializableUsbDevice) {
            SerializableUsbDevice device = (SerializableUsbDevice) obj;
            return vendorId == device.getVendorId() && deviceId == device.getDeviceId();
        }
        return false;
    }
}
