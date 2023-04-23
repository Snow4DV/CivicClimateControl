package ru.snowadv.civic_climate_control;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
        Intent restartClimateServiceIntent =
                new Intent(context, ClimateOverlayService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                restartClimateServiceIntent, PendingIntent.FLAG_IMMUTABLE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground, context.getString(R.string.restart_service),
                    pendingIntent).build();
            Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(stringResourceId))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(action).build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission
                    (context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions. Fix later!
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                // That is android 13 bs. There's no single car headunit that uses api > 30.
                return;
            }
            notificationManager.notify(0, notification);

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
