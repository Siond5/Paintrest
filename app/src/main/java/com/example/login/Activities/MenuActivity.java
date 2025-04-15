package com.example.login.Activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.login.Classes.Colors;
import com.example.login.Classes.MyUser;
import com.example.login.Fragments.HomeFragment;
import com.example.login.Fragments.PaintFragment;
import com.example.login.Fragments.ProfileFragment;
import com.example.login.Fragments.UserDetailsFragment;
import com.example.login.Notification_classes.NotificationReceiver;
import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Calendar;

public class MenuActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    SharedPreferences sp;
    private MyUser user;
    private Colors color;
    private int bgColor, btnColor;
    private int itemId;
    private ProgressBar progressBar;
    private boolean isUserLoaded = false;
    private boolean isColorLoaded = false;
    private boolean isProfileImageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        bottomNavigationView = findViewById(R.id.bottom_navigation_menu);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE); // Show loading screen
        loadFromFirebase(this);
        NotificationReceiver.createNotificationChannel(this);  // Make sure to create the notification channel

        bottomNavigationView.setOnItemSelectedListener(
                new NavigationBarView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        itemId = item.getItemId();

                        if (itemId == R.id.menu_home) {
                            createFragment(new HomeFragment());
                            return true;
                        } else if (itemId == R.id.menu_settings) {
                            createFragment(new UserDetailsFragment());
                            return true;
                        } else if (itemId == R.id.menu_paint) {
                            createFragment(new PaintFragment());
                            return true;
                        } else if (itemId == R.id.menu_profile) {
                            createFragment(new ProfileFragment());
                            return true;
                        }
                        return false;
                    }
                });

        // Now scheduling the notification in MenuActivity
        scheduleRepeatingNotification(this);
    }

    public void loadFromFirebase(AppCompatActivity activity) {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();

        try {
            store.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                user = documentSnapshot.toObject(MyUser.class);
                SharedPreferences.Editor editor = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE).edit();
                editor.putString("firstName", user.getFirstName());
                editor.putString("lastName", user.getLastName());
                editor.putString("phone", user.getPhone());
                editor.putInt("yob", user.getYob());
                editor.apply();
                isUserLoaded = true;
                checkLoadingComplete();
            });

            store.collection("colors").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                color = documentSnapshot.toObject(Colors.class);
                bgColor = (color != null) ? color.getBackgroundColor() : getContextColor(R.color.Default);
                btnColor = (color != null) ? color.getButtonColor() : getContextColor(R.color.button);
                SharedPreferences.Editor editor = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE).edit();
                editor.putInt("bgColor", bgColor);
                editor.putInt("btnColor", btnColor);
                editor.apply();
                isColorLoaded = true;
                checkLoadingComplete();
            });

            downloadProfileImage(activity);

        } catch (Exception e) {
            Toast.makeText(activity, "Failed to load data", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLoadingComplete() {
        if (isUserLoaded && isColorLoaded && isProfileImageLoaded) {
            progressBar.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            createFragment(new HomeFragment());
        }
    }

    private void createFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public void scheduleRepeatingNotification(Context context) {
        // Ensure the context is valid before checking permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
        }

        // Check if we have permission to set exact alarms (for Android 12/S and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!context.getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
                // Permission not granted, prompt the user to go to settings
                Toast.makeText(context, "Please allow permission to schedule exact alarms", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                context.startActivity(intent);
                return;
            }
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // Set the alarm for 17:00 (5:00 PM) today or the next day if it's already past 17:00
        if (currentHour >= 17) {
            // If current time is 17:00 or later, set for the next day
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Add one day
        }

        // Set the alarm time to 17:00:00 (5:00 PM)
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // Set the exact alarm initially
       // long dayInMillis = 86400000; // 24 hours in milliseconds
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
       // alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),dayInMillis, pendingIntent);
    }

    public void downloadProfileImage(AppCompatActivity activity) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getCurrentUser().getUid();
        StorageReference storageRef = storage.getReference().child("profile_images").child(uid + ".jpg");
        SharedPreferences sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE).edit();
        // Create a local file in the cache directory; change location as needed.
        File localFile = new File(this.getCacheDir(), "profile_" + System.currentTimeMillis() + ".jpg");
        storageRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                            Uri localUri = Uri.fromFile(localFile);
                            String photoUri = localUri.toString();
                            editor.putString("profileImageUri", photoUri);
                            editor.apply();
                        })
         .addOnFailureListener(e ->        sharedPreferences.edit().remove("profileImageUri").apply() );

        isProfileImageLoaded = true;
        checkLoadingComplete();
    }

    public int getContextColor(int color) {
        return ContextCompat.getColor(this, color);
    }
}
