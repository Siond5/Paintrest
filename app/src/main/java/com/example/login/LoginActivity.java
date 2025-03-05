package com.example.login;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    EditText etLoginEmail, etLoginPassword;
    Button btnLogin, goSignup, btnForgotPassword;
    ImageButton btnTogglePassword;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViews();
        btnLogin.setOnClickListener(this);
        goSignup.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this);
        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

    }

    private void findViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        goSignup = findViewById(R.id.goSignup);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            etLoginPassword.setTypeface(Typeface.DEFAULT); // Ensures the default font is used

        } else {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            etLoginPassword.setTypeface(Typeface.DEFAULT); // Ensures the default font is used

        }
        etLoginPassword.setSelection(etLoginPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    @Override
    public void onClick(View view) {
        if (view == goSignup) {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        }

        else if (view == btnLogin) {
            String email = etLoginEmail.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            // Validate email and password fields
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
                return;  // Prevent Firebase authentication call if inputs are invalid
            }

            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            fbAuth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginActivity.this, "One or more of the fields is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        else if (view == btnForgotPassword) {
            LayoutInflater inflater = LayoutInflater.from(LoginActivity.this);
            View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
            EditText inputEmail = dialogView.findViewById(R.id.etEmail);
            inputEmail.setText(etLoginEmail.getText().toString());
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Reset Password")
                    .setMessage("Enter your email here to receive a password reset link")
                    .setView(dialogView)

                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
                        fbAuth.sendPasswordResetEmail(inputEmail.getText().toString());
                        Toast.makeText(LoginActivity.this, "Password reset link was sent to your email", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    }
}
