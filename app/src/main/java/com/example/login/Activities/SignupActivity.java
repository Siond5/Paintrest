package com.example.login.Activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

import com.example.login.Classes.Colors;
import com.example.login.Classes.MyUser;
import com.example.login.Dialogs.LoadingManagerDialog;
import com.example.login.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity implements View.OnClickListener {

    // Declare Views
    private EditText etSignupEmail, etSignupPassword, etSignupFname, etSignupLname, etSignupPhone, etSignupYOB;
    private Button btnSignup, goSignin;
    private FirebaseAuth fbAuth;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        findViews();

        // Set listeners for the buttons
        btnSignup.setOnClickListener(this);
        goSignin.setOnClickListener(this);
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        fbAuth = FirebaseAuth.getInstance();

        setUpTextWatchers();
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
            etSignupPassword.setTypeface(Typeface.DEFAULT);
        } else {
            etSignupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            etSignupPassword.setTypeface(Typeface.DEFAULT);
        }
        etSignupPassword.setSelection(etSignupPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    // Set up text watchers for dynamic validation
    private void setUpTextWatchers() {
        etSignupEmail.addTextChangedListener(createValidationWatcher(etSignupEmail, "email"));
        etSignupPassword.addTextChangedListener(createValidationWatcher(etSignupPassword, "password"));
        etSignupFname.addTextChangedListener(createValidationWatcher(etSignupFname, "name"));
        etSignupLname.addTextChangedListener(createValidationWatcher(etSignupLname, "name"));
        etSignupPhone.addTextChangedListener(createValidationWatcher(etSignupPhone, "phone"));
        etSignupYOB.addTextChangedListener(createValidationWatcher(etSignupYOB, "yearOfBirth"));
    }

    private TextWatcher createValidationWatcher(final EditText editText, final String type) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                switch (type) {
                    case "email":
                        validateEmail(editText.getText().toString());
                        break;
                    case "password":
                        validatePassword(editText.getText().toString());
                        break;
                    case "name":
                        validateName(editText.getText().toString(), editText == etSignupFname ? "first name" : "last name");
                        break;
                    case "phone":
                        validatePhone(editText.getText().toString());
                        break;
                    case "yearOfBirth":
                        validateYearOfBirth(editText.getText().toString());
                        break;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };
    }

    @Override
    public void onClick(View view) {
        if (view == goSignin) {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        if (view == btnSignup) {
            String email = etSignupEmail.getText().toString();
            String pass = etSignupPassword.getText().toString();
            String firstname = etSignupFname.getText().toString();
            String lastname = etSignupLname.getText().toString();
            String phone = etSignupPhone.getText().toString();
            String yobStr = etSignupYOB.getText().toString();

            //valid fields by boolean veriables
            boolean isEmailValid = validateEmail(email);
            boolean isPasswordValid = validatePassword(pass);
            boolean isFirstNameValid = validateName(firstname, "first name");
            boolean isLastNameValid = validateName(lastname, "last name");
            boolean isPhoneValid = validatePhone(phone);
            boolean isYOBValid = validateYearOfBirth(yobStr);

            // If all fields are valid, proceed with Firebase signup
            if (isEmailValid && isPasswordValid && isFirstNameValid && isLastNameValid && isPhoneValid && isYOBValid) {
                LoadingManagerDialog.showLoading(this, "Creating account...");
                // Create user in Firebase Authentication
                fbAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                int yob = Integer.parseInt(yobStr);
                                MyUser user = new MyUser(firstname, lastname, phone, yob);
                                FirebaseFirestore store = FirebaseFirestore.getInstance();
                                store.collection("users")
                                        .document(fbAuth.getCurrentUser().getUid()).set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            store.collection("colors").document(fbAuth.getCurrentUser().getUid()).set(new Colors(SignupActivity.this));
                                            Toast.makeText(SignupActivity.this, "User added successfully!", Toast.LENGTH_SHORT).show();
                                            LoadingManagerDialog.hideLoading();
                                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(SignupActivity.this, "Failed to save User information. Please try again.", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(SignupActivity.this, "Failed to create account. Please check your Email and Password.", Toast.LENGTH_SHORT).show();
                                LoadingManagerDialog.hideLoading();
                            }
                        });
            } else {
                LoadingManagerDialog.hideLoading();
                Toast.makeText(SignupActivity.this, "Please fix the errors above before submitting.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Email validation
    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            etSignupEmail.setError("Please enter your email.");
            return false;
        } else {
            etSignupEmail.setError(null);
            return true;
        }
    }

    // Password validation
    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            etSignupPassword.setError("Please enter your password.");
            return false;
        } else if (password.length() < 6) {
            etSignupPassword.setError("Password must be at least 6 characters.");
            return false;
        } else {
            etSignupPassword.setError(null);
            return true;
        }
    }

    // Name validation (first name or last name)
    private boolean validateName(String name, String fieldName) {
        if (name.isEmpty()) {
            if (fieldName.equals("first name")) {
                etSignupFname.setError("Please enter your first name.");
            } else {
                etSignupLname.setError("Please enter your last name.");
            }
            return false;
        } else {
            if (fieldName.equals("first name")) {
                etSignupFname.setError(null);
            } else {
                etSignupLname.setError(null);
            }
            return true;
        }
    }

    // Phone validation
    private boolean validatePhone(String phone) {
        if (phone.isEmpty()) {
            etSignupPhone.setError("Please enter your phone number.");
            return false;
        } else if (!phone.matches("\\d{9,10}")) {
            etSignupPhone.setError("Phone must be 9-10 digits and contain only numbers.");
            return false;
        } else {
            etSignupPhone.setError(null);
            return true;
        }
    }

    // Year of birth validation
    private boolean validateYearOfBirth(String yobStr) {
        if (yobStr.isEmpty()) {
            etSignupYOB.setError("Please enter your year of birth.");
            return false;
        } else if (yobStr.length() != 4) {
            etSignupYOB.setError("Year of birth must be exactly 4 digits.");
            return false;
        } else {
            try {
                Integer.parseInt(yobStr);
                etSignupYOB.setError(null);
                return true;
            } catch (NumberFormatException e) {
                etSignupYOB.setError("Year of birth must be a valid number.");
                return false;
            }
        }
    }
}
