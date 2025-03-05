package com.example.login;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity implements View.OnClickListener {
    EditText etSignupEmail, etSignupPassword, etSignupFname, etSignupLname, etSignupPhone, etSignupYOB;
    Button btnSignup, goSignin;
    FirebaseAuth fbAuth;
    ImageButton btnTogglePassword;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViews();
        btnSignup.setOnClickListener(this);
        goSignin.setOnClickListener(this);
        fbAuth = FirebaseAuth.getInstance();

        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

    }

    private void findViews() {
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        etSignupFname = findViewById(R.id.etSignupFname);
        etSignupLname = findViewById(R.id.etSignupLname);
        etSignupPhone = findViewById(R.id.etSignupPhone);
        etSignupYOB = findViewById(R.id.etSignupYOB);
        btnSignup = findViewById(R.id.btnSignup);
        goSignin = findViewById(R.id.goSignin);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etSignupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            etSignupPassword.setTypeface(Typeface.DEFAULT); // Ensures the default font is used

        } else {
            etSignupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            etSignupPassword.setTypeface(Typeface.DEFAULT); // Ensures the default font is used

        }
        etSignupPassword.setSelection(etSignupPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }


    @Override
    public void onClick(View view) {
        if (view == goSignin) {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        if (view == btnSignup) {
            try {
                String email = etSignupEmail.getText().toString();
                String pass = etSignupPassword.getText().toString();
                String firstname = etSignupFname.getText().toString();
                String lastname = etSignupLname.getText().toString();
                String phone = etSignupPhone.getText().toString();
                String yobStr = etSignupYOB.getText().toString();

                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (firstname.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter your first name.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (lastname.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter your last name.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phone.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter your phone number.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!phone.matches("\\d{9,10}")) {
                    Toast.makeText(SignupActivity.this, "Phone must be 9-10 digits and contain only numbers.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (yobStr.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter your year of birth.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (yobStr.length() != 4) {
                    Toast.makeText(SignupActivity.this, "Year of birth must be exactly 4 digits.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int yob;
                try {
                    yob = Integer.parseInt(yobStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(SignupActivity.this, "Year of birth must be a valid number.", Toast.LENGTH_SHORT).show();
                    return;
                }

                fbAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    try {
                                        MyUser user = new MyUser(firstname, lastname, phone, yob);
                                        Colors color = new Colors(SignupActivity.this);

                                        FirebaseFirestore store = FirebaseFirestore.getInstance();
                                        store.collection("users")
                                                .document(fbAuth.getCurrentUser().getUid()).set(user)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        store.collection("colors").document(fbAuth.getCurrentUser().getUid()).set(color);
                                                        Toast.makeText(SignupActivity.this, "User added successfully!", Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                                                        startActivity(intent);
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(SignupActivity.this, "Failed to save user data. Please try again.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } catch (Exception e) {
                                        Toast.makeText(SignupActivity.this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(SignupActivity.this, "Failed to create account. Please check your credentials.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } catch (Exception e) {
                Toast.makeText(SignupActivity.this, "An unexpected error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}