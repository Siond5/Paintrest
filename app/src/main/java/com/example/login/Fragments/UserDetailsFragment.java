package com.example.login.Fragments;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.login.Activities.LoginActivity;
import com.example.login.Activities.MainActivity;
import com.example.login.Activities.MenuActivity;
import com.example.login.Activities.UiSettingsActivity;
import com.example.login.Classes.MyUser;
import com.example.login.R;
import com.example.login.Activities.ViewPhotoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class UserDetailsFragment extends Fragment implements View.OnClickListener {
    private EditText etDetailsEmail, etDetailsFirstName, etDetailsLastName, etDetailsPhone, etDetailsYOB;
    private Button btnDetailsSave, btnLogout, btnDeleteAccount, btnChangePassword;
    private ImageView ivProfilePicture, btnUserUi;
    private Uri profileImageUri;
    private SharedPreferences sharedPreferences;

    public UserDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_details, container, false);

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        etDetailsEmail = view.findViewById(R.id.etDetailsEmail);
        etDetailsFirstName = view.findViewById(R.id.etDetailsFname);
        etDetailsLastName = view.findViewById(R.id.etDetailsLname);
        etDetailsPhone = view.findViewById(R.id.etDetailsPhone);
        etDetailsYOB = view.findViewById(R.id.etDetailsYOB);
        btnDetailsSave = view.findViewById(R.id.btnSaveDetails);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnUserUi = view.findViewById(R.id.btnUserUi);

        loadBgColor(getActivity(), view);
        loadDetails();
        loadProfilePicture();

        btnDetailsSave.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnDeleteAccount.setOnClickListener(this);
        ivProfilePicture.setOnClickListener(v -> showImageOptions());
        btnChangePassword.setOnClickListener(this);
        btnUserUi.setOnClickListener(this);

        setUpTextWatchers();
        return view;
    }

    private void setUpTextWatchers() {
        etDetailsFirstName.addTextChangedListener(createValidationWatcher(etDetailsFirstName, "name"));
        etDetailsLastName.addTextChangedListener(createValidationWatcher(etDetailsLastName, "name"));
        etDetailsPhone.addTextChangedListener(createValidationWatcher(etDetailsPhone, "phone"));
        etDetailsYOB.addTextChangedListener(createValidationWatcher(etDetailsYOB, "yearOfBirth"));
    }

    private TextWatcher createValidationWatcher(final EditText editText, final String type) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                switch (type) {
                    case "name":
                        validateName(editText.getText().toString(), editText == etDetailsFirstName ? "first name" : "last name");
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
    public void onResume() {
        super.onResume();
        // Refresh the UI when returning from the activity
        loadBgColor(getActivity(), getView());
    }

    // Name validation (first name or last name)
    private boolean validateName(String name, String fieldName) {
        if (name.isEmpty()) {
            if (fieldName.equals("first name")) {
                etDetailsFirstName.setError("Please enter your first name.");
            } else {
                etDetailsLastName.setError("Please enter your last name.");
            }
            return false;
        } else {
            if (fieldName.equals("first name")) {
                etDetailsFirstName.setError(null);
            } else {
                etDetailsLastName.setError(null);
            }
            return true;
        }
    }

    // Phone validation
    private boolean validatePhone(String phone) {
        if (phone.isEmpty()) {
            etDetailsPhone.setError("Please enter your phone number.");
            return false;
        } else if (!phone.matches("\\d{9,10}")) {
            etDetailsPhone.setError("Phone must be 9-10 digits and contain only numbers.");
            return false;
        } else {
            etDetailsPhone.setError(null);
            return true;
        }
    }

    // Year of birth validation
    private boolean validateYearOfBirth(String yobStr) {
        if (yobStr.isEmpty()) {
            etDetailsYOB.setError("Please enter your year of birth.");
            return false;
        } else if (yobStr.length() != 4) {
            etDetailsYOB.setError("Year of birth must be exactly 4 digits.");
            return false;
        } else {
            try {
                Integer.parseInt(yobStr);
                etDetailsYOB.setError(null);
                return true;
            } catch (NumberFormatException e) {
                etDetailsYOB.setError("Year of birth must be a valid number.");
                return false;
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == btnDetailsSave) {

            String firstName = etDetailsFirstName.getText().toString();
            String lastName = etDetailsLastName.getText().toString();
            String phone = etDetailsPhone.getText().toString();
            String yobStr = etDetailsYOB.getText().toString();

            // valid fields by boolean variables
            boolean isFirstNameValid = validateName(firstName, "first name");
            boolean isLastNameValid = validateName(lastName, "last name");
            boolean isPhoneValid = validatePhone(phone);
            boolean isYOBValid = validateYearOfBirth(yobStr);
            if (isFirstNameValid && isLastNameValid && isPhoneValid && isYOBValid) {
                int yob = Integer.parseInt(yobStr);
                FirebaseAuth fbAuth = FirebaseAuth.getInstance();
                String uid = fbAuth.getUid();
                FirebaseFirestore store = FirebaseFirestore.getInstance();
                MyUser user = new MyUser();
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setPhone(phone);
                user.setYob(yob);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("firstName", firstName);
                editor.putString("lastName", lastName);
                editor.putString("phone", phone);
                editor.putInt("yob", yob);
                editor.apply();

                store.collection("users").document(uid).set(user);
                Toast.makeText(getActivity(), "Details saved", Toast.LENGTH_SHORT).show();
            }
        }

        else if (view == btnLogout) {
            Intent intent = new Intent(this.getActivity(), MainActivity.class);
            startActivity(intent);
            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            fbAuth.signOut();
            getActivity().finish();
        }

        else if (view == btnDeleteAccount) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Proceed with account deletion
                        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
                        String uid = fbAuth.getUid();
                        FirebaseFirestore store = FirebaseFirestore.getInstance();
                        try {
                            store.collection("users").document(uid).delete();
                            store.collection("colors").document(uid).delete();
                            fbAuth.getCurrentUser().delete()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getActivity(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(getActivity(), MainActivity.class);
                                            startActivity(intent);
                                            getActivity().finish();
                                        }
                                    });
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Failed to delete account. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Do nothing, just dismiss the dialog
                        dialog.dismiss();
                    })
                    .show();
        }

        else if (view == btnChangePassword) {
            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            fbAuth.sendPasswordResetEmail(fbAuth.getCurrentUser().getEmail());
            Toast.makeText(getActivity(), "Password reset link was sent to your email", Toast.LENGTH_SHORT).show();
        }

        else if (view == btnUserUi) {
            Intent intent = new Intent(getActivity(), UiSettingsActivity.class);
            startActivity(intent);
        }
    }

    private void showImageOptions() {
        String[] options = {"View Profile Picture", "Change Profile Picture", "Delete Profile Picture", "Cancel"};
        new AlertDialog.Builder(getContext())
                .setTitle("Set Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewPhoto();
                    } else if (which == 1) {
                        pickImage();
                    } else if (which == 2) {
                        deletePhoto();
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void pickImage() {
        String[] options = {"Take a Photo", "Pick from Gallery"};
        new AlertDialog.Builder(getContext())
                .setTitle("Change Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Camera
                        initializeCameraUri();
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri);
                        takePhoto.launch(intent);
                    } else if (which == 1) { // Gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickImageFromGallery.launch(intent);
                    }
                })
                .show();
    }

    private void initializeCameraUri() {
        try {
            File photoFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "profile_" + System.currentTimeMillis() + ".jpg");
            profileImageUri = FileProvider.getUriForFile(getActivity(),
                    getActivity().getApplicationContext().getPackageName() + ".provider", photoFile);
        } catch (Exception e) {
            profileImageUri = null;
        }
    }

    // Modified Crop Image Launcher which now triggers an upload to Firebase Storage.
    private ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        // Retrieve the cropped image Uri from UCrop
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if(resultUri != null) {
                            profileImageUri = resultUri;
                            ivProfilePicture.setImageURI(profileImageUri);
                            uploadProfileImage(profileImageUri);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Cropping failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void uploadProfileImage(Uri imageUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getCurrentUser().getUid();
        StorageReference storageRef = storage.getReference().child("profile_images").child(uid + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                    saveProfileImageUrlToSharedPreferences(downloadUri.toString());
                                    Toast.makeText(getActivity(), "Image uploaded successfully.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getActivity(), "Something went wrong, try again later.", Toast.LENGTH_SHORT).show())
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Image upload failed.", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileImageUrlToSharedPreferences(String url) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profileImageUrl", url);
        editor.apply();
    }


    private final ActivityResultLauncher<Intent> takePhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (profileImageUri != null) {
                            // Instead of directly using the image, start the crop process
                            cropImage(profileImageUri);
                        } else {
                            Toast.makeText(getActivity(), "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "An error occurred. Please try again later.", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickImageFromGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if(sourceUri != null){
                        // Start the crop process
                        cropImage(sourceUri);
                    }
                } else {
                    Toast.makeText(getActivity(), "Failed to select photo.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void cropImage(Uri sourceUri) {
        // Create a destination URI for the cropped image in the cache directory
        Uri destinationUri = Uri.fromFile(new File(getActivity().getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));

        // Setup UCrop options: enforcing a square crop; adjust as needed
        UCrop uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)       // Enforce 1:1 aspect ratio; remove if you want free cropping.
                .withMaxResultSize(800, 800);  // Set maximum result dimensions

        // Launch UCrop activity
        Intent uCropIntent = uCrop.getIntent(getActivity());
        cropImageLauncher.launch(uCropIntent);
    }

    private byte[] convertImageToByteArray(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to convert image.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void loadProfilePicture() {
        sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        String savedUriString = sharedPreferences.getString("profileImageUri", null);
        if (savedUriString != null) {
            profileImageUri = Uri.parse(savedUriString);  // Assign the loaded URI
            Glide.with(this)
                    .load(profileImageUri)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .transform(new CircleCrop())
                    .into(ivProfilePicture);
        } else {
            ivProfilePicture.setImageResource(R.drawable.default_profile);
        }
    }

    private void deletePhoto() {
        if (profileImageUri == null) {
            Toast.makeText(getActivity(), "No profile photo to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(profileImageUri.getPath());
        if (file.exists()) {
            if (file.delete()) {
                Log.d("DeletePhoto", "Photo deleted from local storage");
            } else {
                Log.e("DeletePhoto", "Failed to delete photo from local storage");
            }
        }

        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getCurrentUser().getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images/" + uid + ".jpg");

        storageRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("DeletePhoto", "Deleted from Firebase Storage"))
                .addOnFailureListener(e -> Log.e("DeletePhoto", "Failed to delete from Firebase", e));

        sharedPreferences.edit().remove("profileImageUri").apply();
        ivProfilePicture.setImageResource(R.drawable.default_profile);
        profileImageUri = null;
    }

    private void viewPhoto() {
        if (profileImageUri != null) {
            Intent intent = new Intent(getActivity(), ViewPhotoActivity.class);
            intent.putExtra("photoUri", profileImageUri.toString());
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "No profile photo to view.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDetails() {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        try {
            sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
            String firstName = sharedPreferences.getString("firstName", null);
            String lastName = sharedPreferences.getString("lastName", null);
            String phone = sharedPreferences.getString("phone", null);
            int yob = sharedPreferences.getInt("yob", -1);

            if (firstName != null && lastName != null && phone != null && yob != -1) {
                etDetailsEmail.setText(fbAuth.getCurrentUser().getEmail());
                etDetailsFirstName.setText(firstName);
                etDetailsLastName.setText(lastName);
                etDetailsPhone.setText(phone);
                etDetailsYOB.setText(String.valueOf(yob));
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to load details. Please try again later.", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadBgColor(Activity activity, View view) {
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("bgColor", R.color.Default);
        view.setBackgroundColor(color);
        loadBtnColor((ViewGroup) view);
    }

    private void loadBtnColor(ViewGroup rootView) {
        int color = sharedPreferences.getInt("btnColor", R.color.button);
        ColorStateList buttonColor = ColorStateList.valueOf(color);

        for (int i = 0; i < rootView.getChildCount(); i++) {
            View childView = rootView.getChildAt(i);

            // If the view is a button, apply the tint
            if (childView instanceof Button) {
                ((Button) childView).setBackgroundTintList(buttonColor);
            }

            // If the view is a ViewGroup, recursively apply the tint to its children
            else if (childView instanceof ViewGroup) {
                loadBtnColor((ViewGroup) childView);
            }
        }
    }
}