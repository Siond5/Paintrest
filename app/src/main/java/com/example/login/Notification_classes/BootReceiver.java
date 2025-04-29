package com.example.login.Notification_classes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/**
 * BroadcastReceiver that listens for device boot completion and reschedules daily notifications.
 */
public class BootReceiver extends BroadcastReceiver {
    /**
     * Tag for logging.
     */
    private static final String TAG = "BootReceiver";

    /**
     * Called when the BroadcastReceiver receives an Intent broadcast.
     * <p>
     * If the broadcast action is BOOT_COMPLETED, it reschedules the daily notification.
     * </p>
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, rescheduling notifications");
            scheduleDailyNotification(context);
        }
    }

    /**
     * Schedules a daily notification at 17:00 (5:00 PM) local time.
     * <p>
     * Uses exact alarms on Android S and above if permission is granted,
     * otherwise falls back to inexact repeating alarms.
     * </p>
     *
     * @param context The Context used to retrieve the AlarmManager.
     */
    private void scheduleDailyNotification(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // Set time to 17:00 (5:00 PM)
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If current time is past 17:00, schedule for tomorrow
        if ((currentHour > 17) || (currentHour == 17 && currentMinute > 0)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d(TAG, "Setting alarm for: " + calendar.getTime().toString());

        // Use exact alarms on Android S (12) and above if allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Exact alarm scheduled for: " + calendar.getTime().toString());
            } else {
                Log.d(TAG, "Cannot schedule exact alarms - permission not granted");
                alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
        } else {
            // For older Android versions
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
            Log.d(TAG, "Exact alarm scheduled for: " + calendar.getTime().toString());
        }
    }
}
