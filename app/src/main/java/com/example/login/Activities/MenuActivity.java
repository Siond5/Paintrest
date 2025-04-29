/**
 * Main menu activity for the app.
 *
 * Initializes bottom navigation, loads user data and colors from Firebase,
 * downloads the profile image, schedules daily notifications, and
 * handles fragment navigation based on menu selections.
 */
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.util.Calendar;

public class MenuActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    private MyUser user;
    private Colors color;
    private int bgColor, btnColor;
    private int itemId;
    private ProgressBar progressBar;
    private boolean isUserLoaded = false;
    private boolean isColorLoaded = false;
    private boolean isProfileImageLoaded = false;

    /**
     * Called when the activity is created.
     * Sets the layout, initializes views, starts data loading and notification scheduling.
     *
     * @param savedInstanceState Saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        bottomNavigationView = findViewById(R.id.bottom_navigation_menu);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE); // Show loading screen
        loadFromFirebase(this);
        NotificationReceiver.createNotificationChannel(this);

        bottomNavigationView.setOnItemSelectedListener(
            new NavigationBarView.OnItemSelectedListener() {
                /**
                 * Handles bottom navigation item selection to switch fragments.
                 *
                 * @param item The selected menu item
                 * @return true if the selection was handled
                 */
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

        // Schedule daily notification at 17:00
        scheduleRepeatingNotification(this);
    }

    /**
     * Loads user details, colors, and profile image from Firebase.
     * Stores results in SharedPreferences and updates loading flags.
     *
     * @param activity The calling activity context
     */
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

    /**
     * Checks if all data (user, colors, profile image) has been loaded.
     * Hides the progress bar and shows navigation when loading is complete.
     */
    private void checkLoadingComplete() {
        if (isUserLoaded && isColorLoaded && isProfileImageLoaded) {
            progressBar.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            createFragment(new HomeFragment());
        }
    }

    /**
     * Replaces the fragment container with the specified fragment.
     *
     * @param fragment The fragment to display
     */
    private void createFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    /**
     * Schedules a one-time exact alarm at 17:00 each day to trigger notifications.
     * Requests necessary permissions on Android 12+.
     *
     * @param context The context for AlarmManager and permission checks
     */
    public void scheduleRepeatingNotification(Context context) {
        // Permission checks and alarm scheduling logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!context.getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
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

        if (currentHour >= 17) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    /**
     * Downloads the user's profile image from Firebase Storage into cache.
     * Stores the local URI in SharedPreferences when successful.
     *
     * @param activity The calling activity context
     */
    public void downloadProfileImage(AppCompatActivity activity) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getCurrentUser().getUid();
        StorageReference storageRef = storage.getReference().child("profile_images").child(uid + ".jpg");
        SharedPreferences sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        File localFile = new File(this.getCacheDir(), "profile_" + System.currentTimeMillis() + ".jpg");
        storageRef.getFile(localFile)
            .addOnSuccessListener(taskSnapshot -> {
                Uri localUri = Uri.fromFile(localFile);
                editor.putString("profileImageUri", localUri.toString());
                editor.apply();
            })
            .addOnFailureListener(e -> sharedPreferences.edit().remove("profileImageUri").apply());

        isProfileImageLoaded = true;
        checkLoadingComplete();
    }

    /**
     * Retrieves a color resource as an integer using ContextCompat.
     *
     * @param color The resource ID of the color
     * @return The color int
     */
    public int getContextColor(int color) {
        return ContextCompat.getColor(this, color);
    }
}