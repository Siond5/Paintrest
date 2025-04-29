/**
 * Activity for user sign-up.
 * <p>
 * Handles user input for email, password, first and last name, phone number,
 * and year of birth. Validates input fields dynamically and creates a new
 * user account in Firebase Authentication and Firestore upon successful
 * validation.
 */
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

    // Views for user input
    private EditText etSignupEmail, etSignupPassword, etSignupFname, etSignupLname, etSignupPhone, etSignupYOB;
    private Button btnSignup, goSignin;
    private FirebaseAuth fbAuth;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;

    /**
     * Called when the activity is first created.
     * Sets up edge-to-edge layout, initializes views, listeners, and Firebase Auth.
     *
     * @param savedInstanceState saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Apply window insets for proper layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViews();
        btnSignup.setOnClickListener(this);
        goSignin.setOnClickListener(this);
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        fbAuth = FirebaseAuth.getInstance();
        setUpTextWatchers();
    }

    /**
     * Finds and initializes view references from layout.
     */
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

    /**
     * Toggles visibility of the password field.
     * Updates input type and icon accordingly.
     */
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

    /**
     * Attaches text watchers to input fields for real-time validation.
     */
    private void setUpTextWatchers() {
        etSignupEmail.addTextChangedListener(createValidationWatcher(etSignupEmail, "email"));
        etSignupPassword.addTextChangedListener(createValidationWatcher(etSignupPassword, "password"));
        etSignupFname.addTextChangedListener(createValidationWatcher(etSignupFname, "name"));
        etSignupLname.addTextChangedListener(createValidationWatcher(etSignupLname, "name"));
        etSignupPhone.addTextChangedListener(createValidationWatcher(etSignupPhone, "phone"));
        etSignupYOB.addTextChangedListener(createValidationWatcher(etSignupYOB, "yearOfBirth"));
    }

    /**
     * Creates a TextWatcher for validating specific field types.
     *
     * @param editText the EditText to watch
     * @param type     validation type: "email", "password", "name", "phone", or "yearOfBirth"
     * @return configured TextWatcher
     */
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

    /**
     * Handles button click events for sign-up and navigating to sign-in.
     *
     * @param view the clicked View
     */
    @Override
    public void onClick(View view) {
        if (view == goSignin) {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        }

        if (view == btnSignup) {
            String email = etSignupEmail.getText().toString();
            String pass = etSignupPassword.getText().toString();
            String firstname = etSignupFname.getText().toString();
            String lastname = etSignupLname.getText().toString();
            String phone = etSignupPhone.getText().toString();
            String yobStr = etSignupYOB.getText().toString();

            boolean isEmailValid = validateEmail(email);
            boolean isPasswordValid = validatePassword(pass);
            boolean isFirstNameValid = validateName(firstname, "first name");
            boolean isLastNameValid = validateName(lastname, "last name");
            boolean isPhoneValid = validatePhone(phone);
            boolean isYOBValid = validateYearOfBirth(yobStr);

            if (isEmailValid && isPasswordValid && isFirstNameValid && isLastNameValid && isPhoneValid && isYOBValid) {
                LoadingManagerDialog.showLoading(this, "Creating account...");
                fbAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                int yob = Integer.parseInt(yobStr);
                                MyUser user = new MyUser(firstname, lastname, phone, yob);
                                FirebaseFirestore store = FirebaseFirestore.getInstance();
                                store.collection("users")
                                        .document(fbAuth.getCurrentUser().getUid())
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            store.collection("colors")
                                                    .document(fbAuth.getCurrentUser().getUid())
                                                    .set(new Colors(SignupActivity.this));
                                            Toast.makeText(SignupActivity.this, "User added successfully!", Toast.LENGTH_SHORT).show();
                                            LoadingManagerDialog.hideLoading();
                                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(SignupActivity.this, "Failed to save user information. Please try again.", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(SignupActivity.this, "Failed to create account. Please check your email and password.", Toast.LENGTH_SHORT).show();
                                LoadingManagerDialog.hideLoading();
                            }
                        });
            } else {
                LoadingManagerDialog.hideLoading();
                Toast.makeText(SignupActivity.this, "Please fix the errors above before submitting.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Validates email format and emptiness.
     *
     * @param email the email string to validate
     * @return true if valid, false otherwise
     */
    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            etSignupEmail.setError("Please enter your email.");
            return false;
        } else {
            etSignupEmail.setError(null);
            return true;
        }
    }

    /**
     * Validates password length and emptiness.
     *
     * @param password the password string to validate
     * @return true if at least 6 characters, false otherwise
     */
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

    /**
     * Validates that a name field is not empty.
     *
     * @param name      the name string to validate
     * @param fieldName descriptor for error messaging ("first name" or "last name")
     * @return true if not empty, false otherwise
     */
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

    /**
     * Validates phone number format (digits only, length 9-10).
     *
     * @param phone the phone number string to validate
     * @return true if valid, false otherwise
     */
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

    /**
     * Validates year of birth format (exactly 4 digits, numeric).
     *
     * @param yobStr the year of birth string to validate
     * @return true if a valid 4-digit number, false otherwise
     */
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