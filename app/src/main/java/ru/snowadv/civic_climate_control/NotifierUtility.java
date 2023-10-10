package ru.snowadv.civic_climate_control;

import static android.content.Context.NOTIFICATION_SERVICE;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import ru.snowadv.civic_climate_control.overlay.LayoutClimateOverlay;

// TODO: Consider calling
//    ActivityCompat#requestPermissions. Fix ASAP!
// here to request the missing permissions, and then overriding
//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                          int[] grantResults)
// to handle the case where the user grants the permission. See the documentation
// for ActivityCompat#requestPermissions for more details.
public class NotifierUtility {

    private static final String ERROR_CHANNEL_ID = "Errors_notification_channel";
    private static final String CLIMATE_CHANNEL_ID = "Climate_notification_channel";
    private NotificationChannel errorNotificationChannel;
    private NotificationChannel climateNotificationChannel;
    private NotificationManager notificationManager;

    public NotifierUtility(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannelsAndManager(context);
        }
    }


    public void reportErrorInNotification(Context context, int stringResourceId) {
        Intent restartClimateServiceIntent =
                new Intent(context, LayoutClimateOverlay.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                restartClimateServiceIntent, PendingIntent.FLAG_IMMUTABLE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification.Action.Builder builder = new Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_launcher_foreground),
                    context.getString(R.string.restart_service),
                    pendingIntent);
            Notification notification = new Notification.Builder(
                    context, errorNotificationChannel.getId())
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
            Notification notification = new NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(stringResourceId))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(action).build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission
                    (context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            notificationManager.notify(0, notification);

        }

    }


    //TODO: fix rate limiting for notifications
    public void showNewStatus(Context context, String text, int icon) {
        //Icon largeIcon = Icon.createWithResource(context, icon);
        // api 21 support
        Bitmap largeIconBitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_fan_dir_down);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification notification = new Notification.Builder(
                    context, climateNotificationChannel.getId())
                    .setContentText(text)
                    .setLargeIcon(largeIconBitmap)
                    .setSmallIcon(icon)
                            .build();
            notificationManager.notify(2, notification);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Notification notification = new NotificationCompat.Builder(context, CLIMATE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setLargeIcon(largeIconBitmap)// TODO fix large icon on bad apis
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission
                    (context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            notificationManager.notify(2, notification);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initNotificationChannelsAndManager(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        errorNotificationChannel = createSoundlessNotificationChannel(ERROR_CHANNEL_ID,
                context.getString(R.string.overlay_notification_channel_name), NotificationManager.IMPORTANCE_LOW
        );

        climateNotificationChannel = createSoundlessNotificationChannel(CLIMATE_CHANNEL_ID,
                context.getString(R.string.status_notification_channel_name), NotificationManager.IMPORTANCE_HIGH
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel createSoundlessNotificationChannel(String id, CharSequence name, int importance) {
        var channel = new NotificationChannel(id, name, importance);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
        return channel;
    }


    /*
    Bitmap largeIconBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_fan_dir_down)
    fails with failed to create image decode with message 'unimplemented'
    so here is another approach
     */
    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
