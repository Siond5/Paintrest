package com.example.login;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "default_channel";  // Notification channel ID

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver", "Alarm triggered!");
        sendNotification(context);
        MenuActivity menuActivity = new MenuActivity();
        menuActivity.scheduleRepeatingNotification(context);
    }

    public static void sendNotification(Context context) {
        // Check if notification permission is granted (Android 13 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted, log and show toast
                Log.d("NotificationReceiver", "Notification permission not granted");
                Toast.makeText(context, "Notification permission is not granted", Toast.LENGTH_SHORT).show();
                return;  // Exit if permission is not granted
            }
        }

        try {
            // Create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Daily reminder")
                    .setContentText("This is a your reminder to paint today")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            // Get the NotificationManager system service
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(1, builder.build());
            Log.d("NotificationReceiver", "Notification sent!");
        } catch (SecurityException e) {
            Log.e("NotificationReceiver", "SecurityException: Permission might not be granted", e);
        }
    }

    // This method creates the notification channel for Android 8.0 (API 26) and above
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Default Channel";
            String description = "Channel for general notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
