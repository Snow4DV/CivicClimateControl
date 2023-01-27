package ru.snowadv.civic_climate_control;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class SerializableUsbDevice {
    private String vendorId;
    private String deviceId;
    private String productName;
    private static final Gson gson = new Gson();

    public String getVendorId() {
        return vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getProductName() {
        return productName;
    }

    public SerializableUsbDevice(String vendorId, String deviceId, String productName) {
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.productName = productName;
    }

    public SerializableUsbDevice(UsbDevice usbDevice) {
        this.vendorId = String.valueOf(usbDevice.getVendorId());
        this.deviceId = String.valueOf(usbDevice.getDeviceId());
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
}
