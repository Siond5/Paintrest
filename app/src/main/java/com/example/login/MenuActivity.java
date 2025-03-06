package com.example.login;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Calendar;

public class MenuActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    SharedPreferences sp;
    private MyUser user;
    private Colors color;
    private int bgc;
    private int itemId;
    private ProgressBar progressBar;
    private boolean isUserLoaded = false;
    private boolean isColorLoaded = false;

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
                        } else if (itemId == R.id.menu_userdetails) {
                            createFragment(new UserDetailsFragment());
                            return true;
                        } else if (itemId == R.id.menu_paint) {
                            createFragment(new PaintFragment());
                            return true;
                        } else if (itemId == R.id.menu_settings) {
                            createFragment(new SettingsFragment());
                            return true;
                        }
                        return false;
                    }
                });

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
                bgc = (color != null) ? color.getBackgroundColor() : 0;
                SharedPreferences.Editor editor = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE).edit();
                editor.putInt("color", bgc);
                editor.apply();
                isColorLoaded = true;
                checkLoadingComplete();
            });

        } catch (Exception e) {
            Toast.makeText(activity, "Failed to load data", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLoadingComplete() {
        if (isUserLoaded && isColorLoaded) {
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

    void scheduleRepeatingNotification(Context context) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        //calendar.add(Calendar.MINUTE, 1); // First notification 1 minute from now
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        // Set the exact alarm initially
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

    }

}
