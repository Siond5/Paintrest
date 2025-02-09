package com.example.login;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    SharedPreferences sp;
    private MyUser user;
    private Colors color;
    private Image image;
    private int bgc;
    private int itemId;
    private ProgressBar progressBar;
    private boolean isUserLoaded = false;
    private boolean isColorLoaded = false;
    private boolean isImageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        bottomNavigationView = findViewById(R.id.bottom_navigation_menu);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE); // Show loading screen
        loadFromFirebase(this);

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
    }

    public void loadFromFirebase(Activity activity) {
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

            store.collection("images").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                image = documentSnapshot.toObject(Image.class);
                SharedPreferences.Editor editor = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE).edit();

                if (image != null) {
                    editor.putString("profileImage", image.getProfileImage());
                    editor.putString("profileImageUri", image.getProfileImageUri());
                } else {
                    editor.putString("profileImage", "");
                    editor.putString("profileImageUri", "");
                }
                editor.apply();
                isImageLoaded = true;
                checkLoadingComplete();
            });

        } catch (Exception e) {
            Toast.makeText(activity, "Failed to load data", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLoadingComplete() {
        if (isUserLoaded && isColorLoaded && isImageLoaded) {
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

}
