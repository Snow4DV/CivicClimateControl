package ru.snowadv.civic_climate_control.Adapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UsbConnectionStateChangedBroadcastReceiver extends BroadcastReceiver {

    private final UsbConnectionCallback callback;

    public UsbConnectionStateChangedBroadcastReceiver(UsbConnectionCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        callback.callback();
    }



    @FunctionalInterface
    protected interface UsbConnectionCallback {
        void callback();
    }
}
