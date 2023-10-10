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
import ru.snowadv.civic_climate_control.SerializableUsbDevice;
import ru.snowadv.civic_climate_control.SettingsActivity;
import ru.snowadv.civic_climate_control.adapter.AdapterService;
import ru.snowadv.civic_climate_control.adapter.AdapterState;


/**
 * This activity is responsible for overlay. Made using layouts
 */
public class LayoutClimateOverlay extends Service implements AdapterService.OnNewStateReceivedListener, ServiceConnection {

    static final String CHANNEL_ID = "Overlay_notification_channel";
    private static boolean climateActivityIsVisible = false;

    private static final int LayoutParamFlags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    private static final String TAG = "CivicOverlayService";

    private LayoutInflater inflater;
    private View layoutView;
    private Display mDisplay;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;

    private TextView tempTextView1, tempTextView2;
    private View tempBackground1, tempBackground2;
    private View autoGlyph, acOnGlyph, acOffGlyph, windshieldGlyph;
    private ImageView fanSpeedView, fanDirectionView;


    private NotificationManager notificationManager;
    private NotificationChannel notificationChannel;
    private NotifierUtility notifierUtility;
    private TimerThread thread;
    private AdapterState lastState;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: creating overlay service");
        notifierUtility = new NotifierUtility(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkIfServiceCanDrawOverlays()) {
            Log.e(TAG, "onCreate: service cant draw overlay. stopping");
            stop(this);
        }


        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                LayoutParamFlags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();
        inflater = LayoutInflater.from(this);
        layoutView = inflater.inflate(getThemeFromSharedPrefs().getLayoutId(), null);

        initViewFields(layoutView);

        windowManager.addView(layoutView, params);

        setHeightFromSharedPrefs();

        //This is needed to keep the service running in background just needs a notification to call with startForeground();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannel();
            startNotification();
        }

        if(getSecondsToCloseFromPreferences() != 0) { // hide at start
            layoutView.post(() -> {
                layoutView.setAlpha(0.0f);
                ViewGroup.LayoutParams layoutParams = layoutView.getLayoutParams();
                layoutParams.height = 0;
                layoutParams.width = 0;
            });
        }

        AdapterService.initAdapterService(this, this);
    }



    private void setHeightFromSharedPrefs() {
        int overlay_height = PreferenceManager
                .getDefaultSharedPreferences(this).getInt("overlay_height", 110);
        View overlayLayout = layoutView.findViewById(R.id.overlay_layout);
        if(overlayLayout == null) {
            Log.d(TAG, "setHeightFromSharedPrefs: not supported for this layout");
            return;
        }
        ViewGroup.LayoutParams layoutParams = overlayLayout.getLayoutParams();
        layoutParams.height = overlay_height;
    }

    private SettingsActivity.Theme getThemeFromSharedPrefs() {
        String selectedSkinString = PreferenceManager
                .getDefaultSharedPreferences(this).getString("selected_skin", "HONDA_OVERLAY");
        return SettingsActivity.Theme.valueOf(selectedSkinString);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkIfServiceCanDrawOverlays() {
        if(!Settings.canDrawOverlays(this)) {
            SharedPreferences.Editor preferencesEditor = PreferenceManager
                    .getDefaultSharedPreferences(this).edit();
            preferencesEditor.putBoolean("floating_panel_enabled", false);
            preferencesEditor.apply();
            return false;
        } else {
            return true;
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startNotification() {
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID);
        builder.setContentTitle(getString(R.string.overlay_notification_channel_name)).setContentText(getString(R.string.overlay_running));
        startForeground(1, builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.overlay_notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setSound(null, null);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: stopping overlay service");
        windowManager.removeView(layoutView);
        if(AdapterService.isAlive()) {
            try {
                unbindService(this);
            } catch(IllegalArgumentException ex) {
                Log.e(TAG, "onDestroy: unable to kill service");
                ex.printStackTrace();
            }
        }
        notifierUtility.reportErrorInNotification(this, R.string.service_died);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Toast.makeText(this, "Civic climate service was killed due to low memory", Toast.LENGTH_LONG).show();
    }


    public static ComponentName start(Context context) {
        return context.startService(new Intent(context, LayoutClimateOverlay.class));
    }

    public static boolean stop(Context context) {
        return context.stopService(new Intent(context, LayoutClimateOverlay.class));
    }

    public static void changeServiceState(boolean newState, Context context) {
        if(newState) {
            start(context);
        } else {
            stop(context);
        }
    }

    private int getSecondsToCloseFromPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("floating_panel_duration", 3);
    }

    @Override
    public void onNewAdapterStateReceived(AdapterState newState) { // it runs in service's thread
        if(newState == null || climateActivityIsVisible) {
            return;
        }

        int secondsToCloseFromPreferences = getSecondsToCloseFromPreferences();

        if(lastState == null) {
            lastState = newState;
            return;
        } else if((!lastState.equals(newState)) && secondsToCloseFromPreferences != 0) {
            if(thread != null) {
                thread.interrupt();
            }

            thread = new TimerThread(secondsToCloseFromPreferences);
            thread.start();

            lastState = newState;
        }


        tempBackground1.post(() -> tempBackground1.setVisibility(newState.getTempLeftVisibility() ?
                View.VISIBLE : View.GONE));
        tempTextView1.post(() -> tempTextView1.setVisibility(newState.getTempLeftVisibility() ?
                View.VISIBLE : View.GONE));
        tempTextView1.post(() -> tempTextView1.setText(newState.getTempLeftString()));


        tempBackground2.post(() -> tempBackground2.setVisibility(newState.getTempRightVisibility() ?
                View.VISIBLE : View.GONE));
        tempTextView2.post(() -> tempTextView2.setVisibility(newState.getTempRightVisibility() ?
                View.VISIBLE : View.GONE));
        tempTextView2.post(() -> tempTextView2.setText(newState.getTempRightString()));

        acOnGlyph.post(() -> acOnGlyph.setAlpha(newState.getAcState() ==
                AdapterState.ACState.ON ? 1.0f : 0.0f));
        acOffGlyph.post(() -> acOffGlyph.setAlpha(newState.getAcState() ==
                AdapterState.ACState.OFF ? 1.0f : 0.0f));

        autoGlyph.post(() -> autoGlyph.setVisibility(newState.getAuto() ? View.VISIBLE : View.GONE));
        fanSpeedView.post(() -> fanSpeedView.setImageResource(newState.getFanLevel().getResourceId()));
        fanDirectionView.post(() -> fanDirectionView.setImageResource(newState.getFanDirection().getResourceId()));
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


    private void initViewFields(View layoutView) {
        tempTextView1=layoutView.findViewById(R.id.temp_1);
        tempTextView2=layoutView.findViewById(R.id.temp_2);
        tempBackground1=layoutView.findViewById(R.id.temp_1_background);
        tempBackground2=layoutView.findViewById(R.id.temp_2_background);
        autoGlyph = layoutView.findViewById(R.id.auto_glyph);
        acOnGlyph = layoutView.findViewById(R.id.ac_on_glyph);
        acOffGlyph = layoutView.findViewById(R.id.ac_off_glyph);
        windshieldGlyph = layoutView.findViewById(R.id.windshield_heating);
        fanSpeedView = layoutView.findViewById(R.id.fan_speed);
        fanDirectionView = layoutView.findViewById(R.id.fan_direction);
    }



    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected: connected to adapter service");
        AdapterService.AdapterBinder binder = (service instanceof AdapterService.AdapterBinder) ?
                (AdapterService.AdapterBinder) service : null;
        if(binder == null) {
            Log.e(TAG, "onServiceConnected: adapter service connection failed");
            notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
            stopSelf();
        } else {
            binder.registerListener(this);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died");
        notifierUtility.reportErrorInNotification(this, R.string.adapter_not_connected);
        stopSelf();

    }

    private class TimerThread extends Thread {

        private final int waitMillis;

        private boolean interrupted = false;

        public void interrupt() {
            interrupted = true;
        }

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }

        public TimerThread(int secondsToClose) {
            waitMillis = secondsToClose * 1000;
        }

        @Override
        public void run() {
            if(layoutView == null) return;
            layoutView.post(() -> {
                layoutView.setAlpha(1.0f);
                ViewGroup.LayoutParams layoutParams = layoutView.getLayoutParams();
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            });
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Log.d(TAG, "timerThread: sleep interrupted. length was " + waitMillis);
            }
            if(!isInterrupted()) {
                layoutView.post(() -> {
                    layoutView.setAlpha(0.0f);
                    ViewGroup.LayoutParams layoutParams = layoutView.getLayoutParams();
                    layoutParams.height = 0;
                    layoutParams.width = 0;
                });
            }
        }
    }

    public static void setClimateActivityIsVisible(boolean climateActivityIsVisible) {
        LayoutClimateOverlay.climateActivityIsVisible = climateActivityIsVisible;
    }
}