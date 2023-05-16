package ru.snowadv.civic_climate_control.Adapter;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.snowadv.civic_climate_control.NotifierUtility;
import ru.snowadv.civic_climate_control.R;
import ru.snowadv.civic_climate_control.SerializableUsbDevice;

/**
 * This service is responsible for adapter connection.
 *
 * Behaviour:
 * 1) Alive only when it is connected to device
 * 2) Customer binds to it with Context.bindService()
 */
public class AdapterService extends Service implements SerialInputOutputManager.Listener {
    private static final String ACTION_USB_PERMISSION =
            "ru.snowadv.civic_climate_control.Adapter.AdapterService.USB_PERMISSION";
    private static final String TAG = "AdapterService";
    private static boolean isAlive = false;
    private UsbManager usbManager;
    private final AdapterBinder binder = new AdapterBinder();
    private UsbConnectionStateChangedBroadcastReceiver usbDetachBroadcastReceiver;
    private UsbConnectionAllowedBroadcastReceiver usbConnectionAllowedReceiver;
    private final TreeMap<Integer,OnNewStateReceivedListener> listeners = new TreeMap<>();
    private static int newListenerId = 0; // Used to give unique listeners' IDs
    private UsbDevice currentDevice;
    private UsbDeviceConnection currentConnection = null;
    private SerialInputOutputManager usbIoManager = null;

    private final Gson gson = new Gson();
    private StringBuilder buildingResponse = new StringBuilder();
    private int retries = 0;

    private DelayedDestroyThread destroyThread = null;


    public static boolean isAlive() {
        return isAlive;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: service is being created");
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbDetachBroadcastReceiver
                = detachUsbBroadcastReceiver(this::onUsbDetach);
    }

    /**
     * On start service tries to connect to adapter. You should start receiving states after
     * service start.
     *
     * If everything is fine: service starts and instantly connects - everything is fine
     * @param intent The Intent supplied to {@link android.content.Context#startService},
     * as given. It should have parceled UsbDevice as extra inside intent with key <em>"device"</em>
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     * start.  Use with {@link #stopSelfResult(int)}.
     *
     * @return START_STICKY is returned
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: service is starting");
        if(intent == null) {
            unbindClientsAndStop();
            Log.e(TAG, "onStartCommand: INTENT IS NULL");
        }
        isAlive = true;
        currentDevice = intent.getParcelableExtra("device");
        connectToDevice(currentDevice);
        destroyThread = new DelayedDestroyThread();
        destroyThread.start();
        return START_STICKY;
    }

    private void onUsbDetach() {
        if(!usbManager.getDeviceList().containsValue(currentDevice)) {
            unbindClientsAndStop();
            Toast.makeText(this, getString(R.string.adapter_not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        isAlive = false;
        Log.d(TAG, "onDestroy: service is destroyed");
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
        //registerReceiver(usbConnectionStateChangedBroadcastReceiver, filter);
        //TODO: detach usb should be registered correctly
        return usbConnectionStateChangedBroadcastReceiver;
    }


    /**
     * Method to connect service to UsbDevice
     * P.S. <em>permission to it should already be obtained</em>
     * @param device device that service will be connected to
     *
     * @return Was the connection successful or not
     */
    private boolean connectToDevice(UsbDevice device) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        currentConnection = manager.openDevice(device);
        if (currentConnection == null) {
            Log.e(TAG, "connectToDevice: connection couldn't be established");
            unbindClientsAndStop();
            return false;
        }


        ProbeTable adapterProbeTable = new ProbeTable();

        adapterProbeTable.addProduct(currentDevice.getVendorId(), currentDevice.getProductId(),
                Ch34xSerialDriver.class); // TODO: add driver selection

        UsbSerialProber prober = (true ? UsbSerialProber.getDefaultProber() : new UsbSerialProber(adapterProbeTable));

        UsbSerialDriver usbSerialDriver = prober
                .findAllDrivers(manager).stream()
                .filter((driver) -> driver.getDevice().equals(device))
                .findAny()
                .orElse(null);

        if(usbSerialDriver == null) {
            Log.e(TAG, "connectToDevice: driver not found");
            unbindClientsAndStop();
            return false;
        }

        UsbSerialPort port = usbSerialDriver.getPorts().get(0); // Device should have one port

        try {
            port.open(currentConnection);
            port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE);
            usbIoManager =
                    new SerialInputOutputManager(port, this);
            usbIoManager.start();
            Log.d(TAG, "connectToDevice: usbIoManager started");
            return true;
        } catch(IOException exception) {
            Log.e(TAG, "connectToDevice: failed to connect to device",exception);
            unbindClientsAndStop();
            return false;
        }


    }

    private int registerListener(OnNewStateReceivedListener listener) {
        int id = newListenerId++;
        listeners.put(id, listener);
        return id;
    }


    /**
     * This method is used to start service and bind it to received context.
     * @param context context for binding
     * @param device device with which connection should be made
     * @param connection ServiceConnection object that acts as listener when service starts
     */
    public static void getAccessAndBindService(Context context, SerializableUsbDevice device,
                                               ServiceConnection connection) {
        getAccessToDevice(context, device, (realDevice) -> {
            if(realDevice == null) { // Connection didn't go well
                Log.e(TAG, "getAccessAndBindService: realDevice is null!");
                connection.onServiceConnected(null, null);
                return false;
            } else {
                Intent intent = new Intent(context, AdapterService.class);
                intent.putExtra("device", realDevice);

                context.startService(intent);
                return context.bindService(intent, connection, BIND_AUTO_CREATE);
            }
        });
    }

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

        if(device == null) {
            Log.e(TAG, "getAccessToDevice: device is null!");
            return;
        }

        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice realDevice = manager.getDeviceList().values().stream()
                .filter(device::isDescribingUsbDevice)
                .findAny()
                .orElse(null);


        if(realDevice == null) {
            Log.e(TAG, "getAccessToDevice: realDevice is null!! Expected: " + device.toJson()
                    + "\nGot these: " + manager.getDeviceList().values().stream().map(usbDevice -> usbDevice.getProductName() +
                    " | PID: " + usbDevice.getProductId() + " | VID: "
                    + usbDevice.getVendorId()).collect(Collectors.toList()));
            onAllowedCallback.apply(null); // device is not connected
            return;
        }


        if(manager.hasPermission(realDevice)) { // If device is present and access is permitted
            onAllowedCallback.apply(realDevice);
            return;
        }

        // Otherwise ask for permission
        UsbConnectionAllowedBroadcastReceiver receiver =
                new UsbConnectionAllowedBroadcastReceiver(onAllowedCallback, context);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(receiver, filter);

        PendingIntent permissionIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            permissionIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
        }
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

    private void sendStateToListeners(AdapterState state) {
        listeners.values().forEach(action -> action.onNewAdapterStateReceived(state));
    }

    private boolean unregisterListener(int id) {
        return listeners.remove(id) != null;
    }
    private void spreadNewState(AdapterState newState) {
        for (OnNewStateReceivedListener listener :
                listeners.values()) {
            listener.onNewAdapterStateReceived(newState);
        }
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onNewData(byte[] data) {

        if(destroyThread != null && !destroyThread.isInterrupted()) {
            destroyThread.interrupt();
        }

        String jsonState = new String(data);
        int startIndex = jsonState.indexOf('^');
        int endIndex = jsonState.indexOf('$');


        jsonState = jsonState.substring(startIndex != -1 ? startIndex + 1 : 0,
                endIndex != -1 ? endIndex : jsonState.length());


        if(startIndex != -1) {
            buildingResponse.setLength(0);
        }

        buildingResponse.append(jsonState);

        if(endIndex != -1) {
            //Log.e(TAG, "onNewData: BUILT " + buildingResponse.toString());
            AdapterState adapterState =
                    new AdapterState(0,
                            0, 0, 0,
                            0, false);
            try {
                adapterState = gson.fromJson(buildingResponse.toString(), AdapterState.class);
            } catch(Exception ex) {
                Log.e(TAG, "onNewData: communication failed when deserializing");
                ex.printStackTrace();
            }
            sendStateToListeners(adapterState);
        }


    }

    @Override
    public void onRunError(Exception e) {
        Log.e(TAG, "onRunError: communication fail. Dying in 500 ms.", e);
        destroyThread = new DelayedDestroyThread();
        destroyThread.start();
    }

    public class DelayedDestroyThread extends Thread {

        private int destroyDelayMillis = 3000;

        public DelayedDestroyThread() {
        }

        public DelayedDestroyThread(int destroyDelayMillis) {
            this.destroyDelayMillis = destroyDelayMillis;
        }

        private boolean interrupted = false;

        public void interrupt() {
            interrupted = true;
        }

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }
        @Override
        public void run() {
            try {
                Thread.sleep(destroyDelayMillis);
            } catch (InterruptedException e) {
            }

            if(!isInterrupted()) {
                Log.d(TAG, "destroythread: KILLING ADAPTER SERVICE - No data/Error occurred");
                unbindClientsAndStop();
            }

        }
    }



    public void unbindClientsAndStop() {
        listeners.values().forEach(OnNewStateReceivedListener::onAdapterDisconnected);
        listeners.clear();
        stopSelf();
    }

    public class AdapterBinder extends Binder {
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
        private final Context context;


        public UsbConnectionAllowedBroadcastReceiver(Function<UsbDevice, Boolean> callback, Context context) {
            this.callback = callback;
            this.context = context;
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            context.unregisterReceiver(this);
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

    public interface OnNewStateReceivedListener {
        void onNewAdapterStateReceived(AdapterState newState);
        void onAdapterDisconnected();
    }


}
