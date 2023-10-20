package ru.snowadv.civic_climate_control;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Objects;

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

    public static @Nullable SerializableUsbDevice fromJson(String json) {
        try {
            return gson.fromJson(json, SerializableUsbDevice.class);
        } catch(JsonSyntaxException exception) {
            Log.e(TAG, "fromJson: error while parsing", exception);
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s. VID: %s, PID: %s", productName, vendorId, productId);
    }

    public boolean isDescribingUsbDevice(UsbDevice device) {
        return vendorId == device.getVendorId() && productId == device.getProductId() && Objects.equals(productName, device.getProductName());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof SerializableUsbDevice device) {
            return vendorId == device.getVendorId() && productId == device.getProductId()
                    && productName.equals(device.getProductName());
        }
        return false;
    }
}
