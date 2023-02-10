package ru.snowadv.civic_climate_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;


/**
 * This activity is responsible for overlay
 */
public class ClimateService extends Service { // TODO: service should be binded to AdapterService

    static final String CHANNEL_ID = "Overlay_notification_channel";

    private static final int LayoutParamFlags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    private static final String TAG = "CivicClimateService";

    private LayoutInflater inflater;
    private View layoutView;
    private Display mDisplay;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;

    private TextView tempTextView1, tempTextView2;
    private View autoGlyph, acOnGlyph, acOffGlyph, windshieldGlyph;
    private ImageView fanSpeedView, fanDirectionView;

    private CycleChangeThread cycleChangeThread;

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
        cycleChangeThread = new CycleChangeThread();
        cycleChangeThread.start();

        //This is needed to keep the service running in background just needs a notification to call with startForeground();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannel();
            startNotification();
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
        cycleChangeThread.interrupt();
        windowManager.removeView(layoutView);
    }

    public static ComponentName start(Context context) {
        return context.startService(new Intent(context, ClimateService.class));
    }

    public static boolean stop(Context context) {
        return context.stopService(new Intent(context, ClimateService.class));
    }

    public static void changeServiceState(boolean newState, Context context) {
        if(newState) {
            start(context);
        } else {
            stop(context);
        }
    }


    private class CycleChangeThread extends Thread {
        int fanLevel = 0;
        int temp = 10;
        int fanDirection = 0;
        boolean windshield = false;
        boolean ac = false;
        boolean auto = false;
        @Override
        public void run() {
            while(!isInterrupted()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if(++fanLevel > 6) fanLevel = 0;
                    if(++temp > 90) temp = 10;
                    if(++fanDirection > 2) fanDirection = 0;

                    windshield = !windshield;
                    ac = !ac;
                    auto = !auto;

                    int[] fanLevels = {R.drawable.ic_fan_speed_1,R.drawable.ic_fan_speed_2,
                            R.drawable.ic_fan_speed_3,R.drawable.ic_fan_speed_4,
                            R.drawable.ic_fan_speed_5,R.drawable.ic_fan_speed_6,
                            R.drawable.ic_fan_speed_7};
                    fanSpeedView.setImageDrawable(getDrawable(fanLevels[fanLevel]));

                    tempTextView1.setText(String.valueOf(temp));
                    tempTextView2.setText(String.valueOf(temp + 1));

                    int[] fanDirections = {R.drawable.ic_fan_dir_up_down,
                            R.drawable.ic_fan_dir_up, R.drawable.ic_fan_dir_down};
                    fanDirectionView.setImageDrawable(getDrawable(fanDirections[fanDirection]));

                    windshieldGlyph.setVisibility(windshield ? View.VISIBLE : View.GONE);
                    acOnGlyph.setVisibility(ac ? View.VISIBLE : View.GONE);
                    acOffGlyph.setVisibility(!ac ? View.VISIBLE : View.GONE);
                    autoGlyph.setVisibility(auto ? View.VISIBLE : View.GONE);

                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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