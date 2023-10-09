package ru.snowadv.civic_climate_control.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import ru.snowadv.civic_climate_control.NotifierUtility;
import ru.snowadv.civic_climate_control.R;
import ru.snowadv.civic_climate_control.SettingsActivity;
import ru.snowadv.civic_climate_control.adapter.AdapterService;
import ru.snowadv.civic_climate_control.adapter.AdapterState;


public class NotificationClimateOverlay extends Service implements AdapterService.OnNewStateReceivedListener, ServiceConnection {
    private static final String TAG = "CivicNotifOvService";

    private NotifierUtility notifierUtility;
    private AdapterState lastState = null;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: creating notification overlay service");
        notifierUtility = new NotifierUtility(this);
        AdapterService.initAdapterService(this, this);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: stopping overlay service");
        if(AdapterService.isAlive()) {
            try {
                unbindService(this);
            } catch(IllegalArgumentException ex) {
                Log.e(TAG, "onDestroy: unable to kill service");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Toast.makeText(this, "Civic climate service was killed due to low memory", Toast.LENGTH_LONG).show();
    }


    public static ComponentName start(Context context) {
        return context.startService(new Intent(context, NotificationClimateOverlay.class));
    }

    public static boolean stop(Context context) {
        return context.stopService(new Intent(context, NotificationClimateOverlay.class));
    }

    public static void changeServiceState(boolean newState, Context context) {
        if(newState) {
            start(context);
        } else {
            stop(context);
        }
    }

    @Override
    public void onNewAdapterStateReceived(AdapterState newState) { // it runs in service's thread
        if(lastState == null || !lastState.equals(newState)) {
            notifierUtility.showNewStatus(this, newState.toDisplayString(this));
            lastState = newState;
        }
    }

    @Override
    public void onAdapterDisconnected() {
        try {
            unbindService(this);
            stopSelf();
        } catch(IllegalArgumentException exception) {
            Log.e(TAG, "onAdapterDisconnected: tried to unbind service, but it is already dead");
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected: connected to adapter service");
        AdapterService.AdapterBinder binder = (service instanceof AdapterService.AdapterBinder) ?
                (AdapterService.AdapterBinder) service : null;
        if(binder == null) {
            Log.e(TAG, "onServiceConnected: adapter service connection failed");
            stopSelf();
        } else {
            binder.registerListener(this);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died");
        stopSelf();

    }
}