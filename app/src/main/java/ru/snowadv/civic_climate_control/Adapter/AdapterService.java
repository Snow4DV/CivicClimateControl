package ru.snowadv.civic_climate_control.Adapter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;


import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.snowadv.civic_climate_control.SerializableUsbDevice;

/**
 * This service is responsible for adapter connection
 */
public class AdapterService extends Service {
    private UsbManager usbManager;

    private final ArrayList<SerializableUsbDevice> connectedDevices = new ArrayList<>();
    private final AdapterBinder binder = new AdapterBinder();

    private UsbConnectionBroadcastReceiver usbConnectionBroadcastReceiver;

    private final ArrayList<OnDeviceListChangeListener> connectionsListeners = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbConnectionBroadcastReceiver
                = attachUsbBroadcastReceiver(this::onConnectedDevicesChange);

        updateDevicesList();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;

    }

    private void onConnectedDevicesChange() {
        updateDevicesList();
        fireListeners();
    }

    public int registerListener(OnDeviceListChangeListener listener) {
        connectionsListeners.add(listener);
        return connectionsListeners.size() - 1;
    }

    public boolean unregisterListener(OnDeviceListChangeListener listener) {
        return connectionsListeners.remove(listener);
    }

    private void fireListeners() {
        for (OnDeviceListChangeListener listener:
             connectionsListeners) {
            listener.onUpdate(connectedDevices);
        }
    }

    public void updateDevicesList() {
        connectedDevices.clear();
        Set<HashMap.Entry<String, UsbDevice>> devices = usbManager.getDeviceList().entrySet();
        for (Map.Entry<String, UsbDevice> device : devices) {
            SerializableUsbDevice serializableUsbDevice =
                    new SerializableUsbDevice(device.getValue());
            connectedDevices.add(serializableUsbDevice);
        }
    }

    private UsbConnectionBroadcastReceiver attachUsbBroadcastReceiver(
            UsbConnectionBroadcastReceiver.UsbConnectionCallback callback) {
        UsbConnectionBroadcastReceiver usbConnectionBroadcastReceiver
                = new UsbConnectionBroadcastReceiver(callback);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbConnectionBroadcastReceiver, filter);
        return usbConnectionBroadcastReceiver;
    }


    private boolean connectToDevice() {
        if(usbManager.getDeviceList().size() == 0) {
            return false;
        }
        return connectToDevice(usbManager.getDeviceList().values().iterator().next());
    }
    private boolean connectToDevice(SerializableUsbDevice serializedDevice) {
        for (UsbDevice device :
                usbManager.getDeviceList().values()) {
            if(serializedDevice.equals(device)) {
                return connectToDevice(device);
            }
        }
        return false;
    }
    private boolean connectToDevice(UsbDevice device) {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return false;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        port.open(connection);
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(usbConnectionBroadcastReceiver);
        return super.onUnbind(intent);
    }

    private class AdapterBinder extends Binder {
        public AdapterService getService() {
            return AdapterService.this;
        }
        private ArrayList<SerializableUsbDevice> getConnectedDevices() {
            return connectedDevices;
        }

        public int registerListener(OnDeviceListChangeListener listener) {
            return AdapterService.this.registerListener(listener);
        }

        public boolean unregisterListener(OnDeviceListChangeListener listener) {
            return AdapterService.this.unregisterListener(listener);
        }
    }

    public interface OnDeviceListChangeListener {
        void onUpdate(List<SerializableUsbDevice> devices);
    }
}
