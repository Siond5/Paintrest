package com.example.login.Notification_classes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule the daily notification when the device boots
            scheduleDailyNotification(context);
        }
    }

    private void scheduleDailyNotification(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, com.example.login.Notification_classes.NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Add one day to schedule for tomorrow
        }

        // Log the time being scheduled for
        Log.d("BootReceiver", "Scheduled time: " + calendar.getTime().toString());

        // Set the repeating alarm to trigger daily at the specified time
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,       // Wake up the device if it's in sleep mode
                calendar.getTimeInMillis(),   // Time to trigger the alarm
                AlarmManager.INTERVAL_DAY,    // Repeating interval of 24 hours (daily)
                pendingIntent                 // PendingIntent that triggers your NotificationReceiver
        );

        Log.d("BootReceiver", "Daily notification scheduled for: " + calendar.getTime().toString());
    }
}
