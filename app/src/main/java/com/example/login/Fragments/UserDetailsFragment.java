/**
 * Fragment to display and manage user details,
 * including editing personal information, managing profile picture,
 * authentication actions, and UI settings.
 */
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.login.Activities.MainActivity;
import com.example.login.Dialogs.LoadingManagerDialog;
import com.example.login.Views.DrawingViewModel;
import com.example.login.Activities.UiSettingsActivity;
import com.example.login.Classes.MyUser;
import com.example.login.R;
import com.example.login.Activities.ViewPhotoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.io.FileOutputStream;

public class UserDetailsFragment extends Fragment implements View.OnClickListener {
    private EditText etDetailsEmail, etDetailsFirstName, etDetailsLastName, etDetailsPhone, etDetailsYOB;
    private Button btnDetailsSave, btnLogout, btnDeleteAccount, btnChangePassword;
    private ImageView ivProfilePicture, btnUserUi;
    private Uri profileImageUri;
    private SharedPreferences sharedPreferences;

    /**
     * Required empty public constructor.
     */
    public UserDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Inflate fragment layout, initialize views, load saved details and picture,
     * set up listeners and text watchers.
     *
     * @param inflater LayoutInflater to inflate the fragment XML
     * @param container Parent view group
     * @param savedInstanceState Bundle of saved state
     * @return Inflated view for this fragment
     */
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

    /**
     * Attach TextWatchers to input fields for live validation.
     */
    private void setUpTextWatchers() {
        etDetailsFirstName.addTextChangedListener(createValidationWatcher(etDetailsFirstName, "name"));
        etDetailsLastName.addTextChangedListener(createValidationWatcher(etDetailsLastName, "name"));
        etDetailsPhone.addTextChangedListener(createValidationWatcher(etDetailsPhone, "phone"));
        etDetailsYOB.addTextChangedListener(createValidationWatcher(etDetailsYOB, "yearOfBirth"));
    }

    /**
     * Create a TextWatcher for a given field type.
     *
     * @param editText EditText to watch
     * @param type     "name", "phone", or "yearOfBirth"
     * @return Configured TextWatcher
     */
    private TextWatcher createValidationWatcher(final EditText editText, final String type) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
        };
    }

    /**
     * Refresh background color when fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadBgColor(getActivity(), getView());
    }

    /**
     * Validate a name field (first or last).
     *
     * @param name      Input name
     * @param fieldName "first name" or "last name"
     * @return true if non-empty, false otherwise
     */
    private boolean validateName(String name, String fieldName) {
        if (name.isEmpty()) {
            if (fieldName.equals("first name")) {
                etDetailsFirstName.setError("Please enter your first name.");
            } else {
                etDetailsLastName.setError("Please enter your last name.");
            }
            return false;
        }
        if (fieldName.equals("first name")) etDetailsFirstName.setError(null);
        else etDetailsLastName.setError(null);
        return true;
    }

    /**
     * Validate phone number format (9-10 digits).
     *
     * @param phone Input phone number
     * @return true if valid digits, false otherwise
     */
    private boolean validatePhone(String phone) {
        if (phone.isEmpty()) {
            etDetailsPhone.setError("Please enter your phone number.");
            return false;
        } else if (!phone.matches("\\d{9,10}")) {
            etDetailsPhone.setError("Phone must be 9-10 digits and contain only numbers.");
            return false;
        }
        etDetailsPhone.setError(null);
        return true;
    }

    /**
     * Validate year of birth format (4-digit number).
     *
     * @param yobStr Input year string
     * @return true if exactly 4 digits and numeric, false otherwise
     */
    private boolean validateYearOfBirth(String yobStr) {
        if (yobStr.isEmpty()) {
            etDetailsYOB.setError("Please enter your year of birth.");
            return false;
        } else if (yobStr.length() != 4) {
            etDetailsYOB.setError("Year of birth must be exactly 4 digits.");
            return false;
        }
        try {
            Integer.parseInt(yobStr);
            etDetailsYOB.setError(null);
            return true;
        } catch (NumberFormatException e) {
            etDetailsYOB.setError("Year of birth must be a valid number.");
            return false;
        }
    }

    /**
     * Handle click events for save, logout, delete account,
     * change password, and UI settings.
     *
     * @param view Clicked view
     */
    @Override
    public void onClick(View view) {
        if (view == btnDetailsSave) {
            String firstName = etDetailsFirstName.getText().toString();
            String lastName = etDetailsLastName.getText().toString();
            String phone = etDetailsPhone.getText().toString();
            String yobStr = etDetailsYOB.getText().toString();
            boolean validName = validateName(firstName, "first name") && validateName(lastName, "last name");
            boolean validContact = validatePhone(phone) && validateYearOfBirth(yobStr);
            if (validName && validContact) {
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
        } else if (view == btnLogout) {
            DrawingViewModel drawingViewModel = new ViewModelProvider(requireActivity()).get(DrawingViewModel.class);
            drawingViewModel.setHasLogOut(true);
            drawingViewModel.reset();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            getActivity().finish();
        } else if (view == btnDeleteAccount) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                    .setPositiveButton("Yes", (dialog, which) -> deleteAccount())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        } else if (view == btnChangePassword) {
            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            fbAuth.sendPasswordResetEmail(fbAuth.getCurrentUser().getEmail());
            Toast.makeText(getActivity(), "Password reset link was sent to your email", Toast.LENGTH_SHORT).show();
        } else if (view == btnUserUi) {
            startActivity(new Intent(getActivity(), UiSettingsActivity.class));
        }
    }

    /**
     * Deletes user account and all associated data from Firebase.
     * This includes profile images, paintings, user info, colors,
     * and painting references from Firestore collections.
     */
    private void deleteAccount() {
        LoadingManagerDialog.showLoading(getActivity(), "Deleting account...");
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        try {
            // Delete profile image
            StorageReference profileRef = storage.getReference().child("profile_images").child(uid + ".jpg");
            profileRef.delete().addOnFailureListener(e -> {
                // Continue even if profile image deletion fails
            });

            // Delete all paintings
            StorageReference paintingsRef = storage.getReference().child("paintings").child(uid);
            paintingsRef.listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference item : listResult.getItems()) {
                            item.delete().addOnFailureListener(e -> {
                                Log.e("DeleteAccount", "Failed to delete painting: " + item.getName(), e);
                            });
                        }

                        // Delete user data from Firestore
                        store.collection("users").document(uid).delete();
                        store.collection("colors").document(uid).delete();

                        // Delete all paintings references
                        store.collection("paintings")
                                .whereEqualTo("uid", uid)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                                        document.getReference().delete();
                                    }

                                    // Finally delete the Firebase Authentication account
                                    fbAuth.getCurrentUser().delete()
                                            .addOnCompleteListener(task -> {
                                                LoadingManagerDialog.hideLoading();
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(getActivity(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                                    DrawingViewModel drawingViewModel = new ViewModelProvider(requireActivity()).get(DrawingViewModel.class);
                                                    drawingViewModel.setHasLogOut(true);
                                                    drawingViewModel.reset();

                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                    getActivity().finish();
                                                } else {
                                                    Toast.makeText(getActivity(), "Failed to delete account. Please try again.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    LoadingManagerDialog.hideLoading();
                                    Toast.makeText(getActivity(), "Failed to delete account data. Please try again.", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        LoadingManagerDialog.hideLoading();
                        Toast.makeText(getActivity(), "Failed to access storage data. Please try again.", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            LoadingManagerDialog.hideLoading();
            Toast.makeText(getActivity(), "Failed to delete account. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show options to view, change, or delete the profile picture.
     */
    private void showImageOptions() {
        String[] options = {"View Profile Picture", "Change Profile Picture", "Delete Profile Picture", "Cancel"};
        new AlertDialog.Builder(getContext())
                .setTitle("Set Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) viewPhoto();
                    else if (which == 1) pickImage();
                    else if (which == 2) deletePhoto();
                    else dialog.dismiss();
                })
                .show();
    }

    /**
     * Prompt user to take a photo or pick from gallery for profile picture.
     */
    private void pickImage() {
        String[] options = {"Take a Photo", "Pick from Gallery"};
        new AlertDialog.Builder(getContext())
                .setTitle("Change Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        initializeCameraUri();
                        if (profileImageUri != null) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri);
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            takePhoto.launch(intent);
                        } else Toast.makeText(getActivity(), "Unable to create file for photo.", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickImageFromGallery.launch(intent);
                    }
                })
                .show();
    }

    /**
     * Create file URI for camera capture.
     */
    private void initializeCameraUri() {
        try {
            File photoFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "profile_" + System.currentTimeMillis() + ".jpg");
            profileImageUri = FileProvider.getUriForFile(
                    getActivity(),
                    getActivity().getApplicationContext().getPackageName() + ".provider",
                    photoFile);
        } catch (Exception e) {
            profileImageUri = null;
            Log.e("InitializeCameraUri", "Error creating image file", e);
        }
    }

    /**
     * Launcher handling crop result and uploading
     * the cropped image URI to Firebase Storage.
     */
    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        profileImageUri = resultUri;
                        ivProfilePicture.setImageURI(profileImageUri);
                        uploadProfileImage(profileImageUri);
                    }
                } else Toast.makeText(getActivity(), "Cropping failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
    );

    /**
     * Uploads an image URI to Firebase Storage under user's profile_images.
     * @param imageUri URI of image to upload
     */
    private void uploadProfileImage(Uri imageUri) {
        LoadingManagerDialog.showLoading(getActivity(), "Uploading image...");
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference().child("profile_images/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ".jpg");
        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            saveProfileImageUriToSharedPreferences(downloadUri.toString());
                            Toast.makeText(getActivity(), "Image uploaded successfully.", Toast.LENGTH_SHORT).show();
                            LoadingManagerDialog.hideLoading();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Something went wrong, try again later.", Toast.LENGTH_SHORT).show();
                            LoadingManagerDialog.hideLoading();
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Image upload failed.", Toast.LENGTH_SHORT).show();
                    LoadingManagerDialog.hideLoading();
                });
    }

    /**
     * Save the Firebase download URL directly to SharedPreferences
     * @param downloadUrl Firebase Storage download URL
     */
    private void saveProfileImageUriToSharedPreferences(String downloadUrl) {
        sharedPreferences.edit().putString("profileImageUri", downloadUrl).apply();
        profileImageUri = Uri.parse(downloadUrl);
    }

    /**
     * Launcher for camera capture result initiating cropping.
     */
    private final ActivityResultLauncher<Intent> takePhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && profileImageUri != null) cropImage(profileImageUri);
                else Toast.makeText(getActivity(), "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
            }
    );

    /**
     * Launcher for gallery pick result initiating cropping.
     */
    private final ActivityResultLauncher<Intent> pickImageFromGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) cropImage(sourceUri);
                } else Toast.makeText(getActivity(), "Failed to select photo.", Toast.LENGTH_SHORT).show();
            }
    );

    /**
     * Start UCrop with square aspect and max size, then launch crop activity.
     * @param sourceUri URI of source image
     */
    private void cropImage(Uri sourceUri) {
        Uri destUri = Uri.fromFile(new File(getActivity().getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));
        UCrop uCrop = UCrop.of(sourceUri, destUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800);
        Intent uCropIntent = uCrop.getIntent(getActivity());
        cropImageLauncher.launch(uCropIntent);  // Use the launcher instead
    }

    /**
     * Load and display the profile picture from SharedPreferences via Glide.
     */
    private void loadProfilePicture() {
        sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        String savedUriString = sharedPreferences.getString("profileImageUri", null);
        if (savedUriString != null) {
            profileImageUri = Uri.parse(savedUriString);
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

    /**
     * Delete the local and remote profile photo, clear preferences, and reset UI.
     */
    private void deletePhoto() {
        if (profileImageUri == null) {
            Toast.makeText(getActivity(), "No profile photo to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(profileImageUri.getPath());
        if (file.exists() && !file.delete()) {
            Log.e("DeletePhoto", "Failed to delete photo from local storage");
        }
        LoadingManagerDialog.showLoading(getActivity(), "Deleting profile picture...");
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getCurrentUser().getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images/" + uid + ".jpg");
        storageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Profile photo deleted", Toast.LENGTH_SHORT).show();
                    LoadingManagerDialog.hideLoading();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to delete profile photo", Toast.LENGTH_SHORT).show();
                    LoadingManagerDialog.hideLoading();
                });
        sharedPreferences.edit().remove("profileImageUri").apply();
        ivProfilePicture.setImageResource(R.drawable.default_profile);
        profileImageUri = null;
    }

    /**
     * Launch ViewPhotoActivity to display the full-size profile photo.
     */
    private void viewPhoto() {
        if (profileImageUri != null) {
            Intent intent = new Intent(getActivity(), ViewPhotoActivity.class);
            intent.putExtra("photoUri", profileImageUri.toString());
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "No profile photo to view.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load user details from SharedPreferences into input fields.
     */
    private void loadDetails() {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
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
    }

    /**
     * Apply saved background color to root view and recursively tint buttons.
     *
     * @param activity Current activity context
     * @param view     Root view to apply background and button colors
     */
    public void loadBgColor(Activity activity, View view) {
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("bgColor", R.color.Default);
        view.setBackgroundColor(color);
        if (view instanceof ViewGroup) {
            loadBtnColor((ViewGroup) view);
        }
    }

    /**
     * Recursively apply tint to all Button instances within the ViewGroup.
     *
     * @param rootView ViewGroup containing buttons to tint
     */
    private void loadBtnColor(ViewGroup rootView) {
        int color = sharedPreferences.getInt("btnColor", R.color.button);
        ColorStateList buttonColor = ColorStateList.valueOf(color);
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);
            if (child instanceof Button) {
                ((Button) child).setBackgroundTintList(buttonColor);
            } else if (child instanceof ViewGroup) {
                loadBtnColor((ViewGroup) child);
            }
        }
    }

}