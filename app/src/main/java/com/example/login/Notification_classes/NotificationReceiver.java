package com.example.login.Notification_classes;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.login.Activities.MainActivity;
import com.example.login.R;

import java.util.Calendar;

/**
 * BroadcastReceiver that handles alarm triggers to send daily notifications and reschedule the next alarm.
 */
public class NotificationReceiver extends BroadcastReceiver {
    /** Notification channel ID for general reminders. */
    private static final String CHANNEL_ID = "default_channel";

    /** Tag for logging. */
    private static final String TAG = "NotificationReceiver";

    /**
     * Called when the alarm triggers. Acquires a wake lock, sends the notification, and schedules the next one.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered!");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp:NotificationWakeLock");
        wakeLock.acquire(10 * 60 * 1000L);
        try {
            sendNotification(context);
            scheduleNextNotification(context);
        } finally {
            wakeLock.release();
        }
    }

    /**
     * Schedules the next daily notification at 17:00 local time.
     *
     * @param context The Context used to retrieve the AlarmManager.
     */
    private void scheduleNextNotification(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent);
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent);
            }
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);
        }
        Log.d(TAG, "Next alarm scheduled for: " + calendar.getTime());
    }

    /**
     * Builds and displays the notification, handling runtime permission checks on Android 13+.
     *
     * @param context The Context used to build and show the notification.
     */
    public static void sendNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission not granted");
                Toast.makeText(context, "Notification permission is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        try {
            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Daily reminder")
                    .setContentText("This is your reminder to paint today")
                    .setSmallIcon(R.drawable.ic_launcher_square)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent!");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Permission might not be granted", e);
        }
    }

    /**
     * Creates the notification channel on Android O and above.
     *
     * @param context The Context used to create the channel.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Default Channel";
            String description = "Channel for general notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
