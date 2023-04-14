package ru.snowadv.civic_climate_control;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class NotifierUtility {

    private static final String CHANNEL_ID = "Errors_notification_channel";
    private NotificationChannel notificationChannel;
    private NotificationManager notificationManager;

    public NotifierUtility(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannelAndManager(context);
        }
    }


    public void reportErrorInNotification(Context context, int stringResourceId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Intent restartClimateServiceIntent =
                    new Intent(context, ClimateOverlayService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                    restartClimateServiceIntent, PendingIntent.FLAG_IMMUTABLE);
            Notification.Action.Builder builder = new Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_launcher_foreground),
                    context.getString(R.string.restart_service),
                    pendingIntent);
            Notification notification = new Notification.Builder(
                    context, notificationChannel.getId())
                    .setContentText(context.getString(stringResourceId))
                    .setContentTitle(context.getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_fan_dir_down)
                    .addAction(builder.build())
                    .build();

            notificationManager.notify(0, notification);
        } else {
            //TODO: implement notifications for pre-O devices.
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initNotificationChannelAndManager(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationChannel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setSound(null, null);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}
