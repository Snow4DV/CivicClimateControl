package ru.snowadv.civic_climate_control.Adapter;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ru.snowadv.civic_climate_control.SerializableUsbDevice;

/**
 * This service is responsible for adapter connection.
 *
 * Behaviour:
 * 1) Alive only when it is connected to device
 * 2) Customer binds to it with Context.bindService()
 * 3) When device disconnected - SEND NOTIFICATION! // TODO   or maybe activites should do it?
 */
public class AdapterService extends Service implements SerialInputOutputManager.Listener {
    /*TODO: problem! There are
        two possible behaviours of service:
        1) It starts, asks for permission
     */
    private static final String ACTION_USB_PERMISSION =
            "ru.snowadv.civic_climate_control.Adapter.AdapterService.USB_PERMISSION";
    private static final String TAG = "AdapterService";
    private static boolean ALIVE = false;
    private UsbManager usbManager;
    private final AdapterBinder binder = new AdapterBinder();
    private UsbConnectionStateChangedBroadcastReceiver usbDetachBroadcastReceiver;
    private UsbConnectionAllowedBroadcastReceiver usbConnectionAllowedReceiver;
    private final TreeMap<Integer,OnNewStateReceivedListener> listeners = new TreeMap<>();
    private static int newListenerId = 0; // Used to give unique listeners' IDs
    private UsbDevice currentDevice;
    private UsbDeviceConnection currentConnection = null;



    @Override
    public void onCreate() {
        super.onCreate();

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbDetachBroadcastReceiver
                = detachUsbBroadcastReceiver(this::onUsbDetach);
    }

    /**
     * On start service tries to connect to adapter. You should start receiving states after
     * service start.
     *
     * WARNING! There are different possible behaviours:
     * 1) Service starts and instantly connects - everything is fine
     * @param intent The Intent supplied to {@link android.content.Context#startService},
     * as given. It should have parceled UsbDevice as extra inside intent with key <em>"device"</em>
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     * start.  Use with {@link #stopSelfResult(int)}.
     *
     *
     * @return START_STICKY is returned
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ALIVE = true;
        currentDevice = intent.getParcelableExtra("device");
        connectToDevice(currentDevice);
        return START_STICKY;
    }

    private void onUsbDetach() {
        if(!usbManager.getDeviceList().containsValue(currentDevice)) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        ALIVE = false;
        spreadNewState(new AdapterState()); // Spread dead state
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private UsbConnectionStateChangedBroadcastReceiver detachUsbBroadcastReceiver(
            UsbConnectionStateChangedBroadcastReceiver.UsbConnectionCallback callback) {
        UsbConnectionStateChangedBroadcastReceiver usbConnectionStateChangedBroadcastReceiver
                = new UsbConnectionStateChangedBroadcastReceiver(callback);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbConnectionStateChangedBroadcastReceiver, filter);
        return usbConnectionStateChangedBroadcastReceiver;
    }


    private void connectToDevice(SerializableUsbDevice serializedDevice) {
        for (UsbDevice device :
                usbManager.getDeviceList().values()) {
            if(serializedDevice.isDescribingUsbDevice(device)) {
                connectToDevice(device);
                return;
            }
        }
    }

    /**
     * Method to connect service to UsbDevice
     * P.S. <em>permission to it should already be obtained</em>
     * @param device
     */
    private void connectToDevice(UsbDevice device) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        UsbDeviceConnection connection = manager.openDevice(device);
        if (connection == null) {
            Log.e(TAG, "connectToDevice: connection couldn't be established");
            stopSelf();
            return;
        }


        ProbeTable adapterProbeTable = new ProbeTable();

        adapterProbeTable.addProduct(currentDevice.getVendorId(), currentDevice.getProductId(),
                Ch34xSerialDriver.class); // TODO: add driver selection - defaulting to ch34x atm

        UsbSerialProber prober = new UsbSerialProber(adapterProbeTable);

        UsbSerialDriver usbSerialDriver = prober
                .findAllDrivers(manager).stream()
                .filter((driver) -> driver.getDevice().equals(device))
                .findAny()
                .orElse(null);

        if(usbSerialDriver == null) {
            Log.e(TAG, "connectToDevice: driver not found");
            stopSelf();
            return;
        }

        UsbSerialPort port = usbSerialDriver.getPorts().get(0); // Device should have one port

        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE);
            SerialInputOutputManager usbIoManager =
                    new SerialInputOutputManager(port, this);
            usbIoManager.start();
        } catch(IOException exception) {
            Log.e(TAG, "connectToDevice: failed to connect to device",exception);
            stopSelf();
        }


    }

    private int registerListener(OnNewStateReceivedListener listener) {
        int id = newListenerId++;
        listeners.put(id, listener);
        return id;
    }


    /*public static void getAccessAndStartService(Context context, SerializableUsbDevice device,
                                                Function<AdapterBinder, Boolean> onStartedCallback) {
        getAccessToDevice(context, device, (realDevice) -> {
            if(realDevice == null) { // Connection didn't go well
                onStartedCallback.apply(null); // Sending null binder - service didn't start
                return false;
            } else {
                Intent intent = new Intent(context, AdapterService.class);
                intent.putExtra("device", realDevice);
                context.bindService(intent, )
                onStartedCallback.apply(manager.openDevice(realDevice));
                return true;
            }
        });
    }*/

    /**
     * Async request to ask for permission and get access to connected device
     * @param context context that will be used to receive broadcasts
     * @param device device to that we will try to connect
     * @param onAllowedCallback callback that will be executed after getting result
     *                          P.S. callback param is <em>@Nullable</em> !
     *                          If it is null - access wasn't granted
     */
    public static void getAccessToDevice(Context context, SerializableUsbDevice device,
                                            Function<UsbDevice, Boolean> onAllowedCallback) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice realDevice = manager.getDeviceList().values().stream()
                .filter(device::isDescribingUsbDevice)
                .findAny()
                .orElse(null);

        if(realDevice == null) {
            onAllowedCallback.apply(null); // device is not connected
            return;
        }


        if(manager.hasPermission(realDevice)) { // If device is present and access is permitted
            onAllowedCallback.apply(realDevice);
            return;
        }

        // Otherwise ask for permission
        UsbConnectionAllowedBroadcastReceiver receiver =
                new UsbConnectionAllowedBroadcastReceiver(onAllowedCallback);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(receiver, filter);

        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        manager.requestPermission(realDevice, permissionIntent);
    }

    private boolean unregisterListener(OnNewStateReceivedListener listener) {
        for (Map.Entry<Integer, OnNewStateReceivedListener> entry:
            listeners.entrySet()){
            if(entry.getValue().equals(listener)) {
                listeners.remove(entry.getKey());
                return true;
            }
        }
        return false;
    }

    private boolean unregisterListener(int id) {
        return listeners.remove(id) != null;
    }
    private void spreadNewState(AdapterState newState) {
        for (OnNewStateReceivedListener listener :
                listeners.values()) {
            listener.onReceive(newState);
        }
    }
    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(usbDetachBroadcastReceiver);
        unregisterReceiver(usbConnectionAllowedReceiver);
        return super.onUnbind(intent);
    }

    @Override
    public void onNewData(byte[] data) {
        Log.d(TAG, "onNewData: " + Arrays.toString(data));
    }

    @Override
    public void onRunError(Exception e) {
        Log.e(TAG, "onRunError: communication fail", e);
    }

    private class AdapterBinder extends Binder {
        public AdapterService getService() {
            return AdapterService.this;
        }

        /**
         * Method to register new listener to get new adapter's states
         * @param listener Method reference to execute with a new adapter state
         * @return Unique ID of listener
         */
        public int registerListener(OnNewStateReceivedListener listener) {
            return AdapterService.this.registerListener(listener);
        }

        /**
         * Method to unregister existing listener
         * @param listener listener that should be removed
         * @return boolean if removal succeeded or not
         */
        public boolean unregisterListener(OnNewStateReceivedListener listener) {
            return AdapterService.this.unregisterListener(listener);
        }

        /**
         * Method to unregister existing listener
         * @param id Unique ID of listener
         * @return boolean if removal succeeded or not
         */
        public boolean unregisterListener(int id) {
            return AdapterService.this.unregisterListener(id);
        }


    }

    /**
     * Broadcast receiver that will check if permission to device was granted and execute a callback
     */
    private static class UsbConnectionAllowedBroadcastReceiver extends BroadcastReceiver {


        private final Function<UsbDevice, Boolean> callback;


        public UsbConnectionAllowedBroadcastReceiver(Function<UsbDevice, Boolean> callback) {
            this.callback = callback;
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            Log.d(TAG, "onReceive: permission was granted");
                            callback.apply(device);
                        }
                    }
                    else {
                        Log.w(TAG, "permission denied for device " + device);
                        callback.apply(null);
                    }
                }
            }
        }


    }

    public static final class AdapterState {
        private final boolean isConnectionAlive;
        String data; // stub

        public boolean isConnectionAlive() {
            return isConnectionAlive;
        }

        /**
         * Constructor creates dead state
         */
        public AdapterState() {
            isConnectionAlive = false;
        }
    }

    @FunctionalInterface
    public interface OnNewStateReceivedListener {
        void onReceive(AdapterState newState);
    }
}
