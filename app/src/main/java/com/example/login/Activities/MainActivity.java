/**
 * Main entry activity for the app.
 * <p>
 * Displays buttons for user sign-up and log-in. If a user is already authenticated
 * via FirebaseAuth, it redirects directly to the MenuActivity.
 */
package com.example.login.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // Buttons to navigate to sign-up or log-in
    Button btnMainSignUp, btnMainLogIn;

    /**
     * Called when the activity is first created.
     * Sets up edge-to-edge layout, initializes views, attaches click listeners,
     * and checks for an existing authenticated user.
     *
     * @param savedInstanceState Saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViews();
        btnMainSignUp.setOnClickListener(this);
        btnMainLogIn.setOnClickListener(this);
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        if (fbAuth.getCurrentUser() != null) {
            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Finds and assigns view references from the layout.
     */
    private void findViews() {
        btnMainSignUp = findViewById(R.id.btnMainSignUp);
        btnMainLogIn = findViewById(R.id.btnMainLogIn);
    }

    /**
     * Handles click events for navigation to sign-up or log-in activities.
     *
     * @param view The clicked view
     */
    @Override
    public void onClick(View view) {
        if (view == btnMainSignUp) {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        } else if (view == btnMainLogIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }
}