package ru.snowadv.civic_climate_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import ru.snowadv.civic_climate_control.Adapter.AdapterService;
import ru.snowadv.civic_climate_control.Adapter.AdapterState;


/**
 * This activity is responsible for overlay   TODO: maybe should receive USB_ATTACH
 */
public class ClimateOverlayService extends Service implements AdapterService.OnServiceStartedListener, AdapterService.OnNewStateReceivedListener {

    static final String CHANNEL_ID = "Overlay_notification_channel";

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
    private View autoGlyph, acOnGlyph, acOffGlyph, windshieldGlyph;
    private ImageView fanSpeedView, fanDirectionView;

    //private CycleChangeThread cycleChangeThread;

    private NotificationManager notificationManager;
    private NotificationChannel notificationChannel;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkIfServiceCanDrawOverlays()) {
            stop(this);
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                LayoutParamFlags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();
        inflater = LayoutInflater.from(this);
        layoutView = inflater.inflate(R.layout.climate_overlay, null);

        initViewFields(layoutView);

        windowManager.addView(layoutView, params);

        //This is needed to keep the service running in background just needs a notification to call with startForeground();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannel();
            startNotification();
        }

        initAdapterService();
    }

    private void initAdapterService() {
        String adapterDeviceJson = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("adapter_name", null);
        SerializableUsbDevice adapterDevice = SerializableUsbDevice.fromJson(adapterDeviceJson);
        AdapterService.getAccessAndBindService(this, adapterDevice, this);
    }


    private void reportErrorInNotification(int stringResourceId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Intent restartClimateServiceIntent =
                    new Intent(this, ClimateOverlayService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0,
                    restartClimateServiceIntent, PendingIntent.FLAG_IMMUTABLE);
            Notification.Action.Builder builder = new Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_launcher_foreground),
                    getString(R.string.restart_service),
                    pendingIntent);
            Notification notification = new Notification.Builder(
                    this, notificationChannel.getId())
                    .setContentText(getString(stringResourceId))
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_fan_dir_down)
                    .addAction(builder.build())
                    .build();

            notificationManager.notify(0, notification);
        } else {
            //TODO: implement notifications for pre-O devices.
        }

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
        //cycleChangeThread.interrupt();
        windowManager.removeView(layoutView);
    }

    public static ComponentName start(Context context) {
        return context.startService(new Intent(context, ClimateOverlayService.class));
    }

    public static boolean stop(Context context) {
        return context.stopService(new Intent(context, ClimateOverlayService.class));
    }

    public static void changeServiceState(boolean newState, Context context) {
        if(newState) {
            start(context);
        } else {
            stop(context);
        }
    }

    @Override
    public void onAdapterServiceStartOrFail(AdapterService.AdapterBinder binder) {
        if(binder == null) {
            Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection failed");
            reportErrorInNotification(R.string.adapter_not_connected);
            stopSelf();
        } else {
            binder.registerListener(this);
        }
    }

    @Override
    public void onAdapterServiceStop() {
        Log.e(TAG, "onAdapterServiceStartOrFail: adapter service connection died");
        reportErrorInNotification(R.string.adapter_not_connected);
        stopSelf();
    }

    @Override
    public void onNewAdapterStateReceived(AdapterState newState) { // it runs in service's thread
        tempTextView1.post(() -> tempTextView1.setText(String.valueOf(newState.getTempLeft())));
        tempTextView2.post(() -> tempTextView2.setText(String.valueOf(newState.getTempRight())));
        acOnGlyph.post(() -> acOnGlyph.setVisibility(newState.isAc() ? View.VISIBLE : View.GONE));
        acOffGlyph.post(() -> acOffGlyph.setVisibility(newState.isAc() ? View.GONE : View.VISIBLE));
        autoGlyph.post(() -> autoGlyph.setVisibility(newState.isAuto() ? View.VISIBLE : View.GONE));
        fanSpeedView.post(() -> fanSpeedView.setImageResource(newState.getFanLevel().getResourceId()));
        fanDirectionView.post(() -> fanDirectionView.setImageResource(newState.getFanDirection().getResourceId()));
//        Log.e(TAG, "onNewAdapterStateReceived: " + new Gson().toJson(newState));
    }




    private void initViewFields(View layoutView) {
        tempTextView1=layoutView.findViewById(R.id.temp_1);
        tempTextView2=layoutView.findViewById(R.id.temp_2);
        autoGlyph = layoutView.findViewById(R.id.auto_glyph);
        acOnGlyph = layoutView.findViewById(R.id.ac_on_glyph);
        acOffGlyph = layoutView.findViewById(R.id.ac_off_glyph);
        windshieldGlyph = layoutView.findViewById(R.id.windshield_heating);
        fanSpeedView = layoutView.findViewById(R.id.fan_speed);
        fanDirectionView = layoutView.findViewById(R.id.fan_direction);
    }
}