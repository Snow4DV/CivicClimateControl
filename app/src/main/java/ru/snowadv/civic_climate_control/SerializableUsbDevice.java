package ru.snowadv.civic_climate_control;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

public class SerializableUsbDevice {
    private static final String TAG = "SerializableUsbDevice";
    private int vendorId;
    private int productId;
    private String productName;
    private static final Gson gson = new Gson();


    public SerializableUsbDevice(int vendorId, int productId, String productName) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.productName = productName;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public static Gson getGson() {
        return gson;
    }

    public SerializableUsbDevice(UsbDevice usbDevice) {
        this.vendorId = usbDevice.getVendorId();
        this.productId = usbDevice.getProductId();
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
        return String.format("%s. VID: %s, PID: %s", productName, vendorId, productId);
    }

    public boolean isDescribingUsbDevice(UsbDevice device) {
        return vendorId == device.getVendorId() && productId == device.getProductId();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof SerializableUsbDevice) {
            SerializableUsbDevice device = (SerializableUsbDevice) obj;
            return vendorId == device.getVendorId() && productId == device.getProductId();
        }
        return false;
    }
}
