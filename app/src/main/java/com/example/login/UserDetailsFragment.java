package com.example.login;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import android.app.Activity;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class UserDetailsFragment extends Fragment implements View.OnClickListener {
    private EditText etDetailsEmail, etDetailsFirstName, etDetailsLastName, etDetailsPhone, etDetailsYOB;
    private Button btnDetailsSave, btnLogout, btnDeleteAccount;
    private ImageView ivProfilePicture;
    private Uri profileImageUri, croppedImageUri;
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

        loadColor(getActivity(), view);
        loadDetails();
       // loadProfilePicture();

        btnDetailsSave.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnDeleteAccount.setOnClickListener(this);
        ivProfilePicture.setOnClickListener(v -> showImageOptions());

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view == btnDetailsSave) {

            String firstName = etDetailsFirstName.getText().toString();
            String lastName = etDetailsLastName.getText().toString();
            String phone = etDetailsPhone.getText().toString();
            String yobStr = etDetailsYOB.getText().toString();

            if (firstName.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter your first name.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lastName.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter your last name.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter your phone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!phone.matches("\\d{9,10}")) {
                Toast.makeText(getActivity(), "Phone must be 9-10 digits and contain only numbers.-", Toast.LENGTH_SHORT).show();
                return;
            }

            if (yobStr.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter your year of birth.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (yobStr.length() != 4) {
                Toast.makeText(getActivity(), "Year of birth must be exactly 4 digits.", Toast.LENGTH_SHORT).show();
                return;
            }

            int yob;
            try {
                yob = Integer.parseInt(yobStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Year of birth must be a valid number.", Toast.LENGTH_SHORT).show();
                return;
            }

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
        } else if (view == btnLogout) {
            Intent intent = new Intent(this.getActivity(), MainActivity.class);
            startActivity(intent);
            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            fbAuth.signOut();
            getActivity().finish();
        } else if (view == btnDeleteAccount) {
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
        File photoFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "profile_" + System.currentTimeMillis() + ".jpg");

        profileImageUri = FileProvider.getUriForFile(
                getActivity(),
                getActivity().getApplicationContext().getPackageName() + ".provider",
                photoFile
        );
    }


    private final ActivityResultLauncher<Intent> takePhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK) {
                    startCrop(profileImageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickImageFromGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    startCrop(selectedImageUri);
                }
            }
    );

    private void startCrop(Uri sourceUri) {
        try {
            File croppedFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "cropped_" + System.currentTimeMillis() + ".jpg");
            croppedImageUri = Uri.fromFile(croppedFile);

            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(sourceUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, croppedImageUri);
            cropIntent.putExtra("outputFormat", "JPEG");

            cropImage.launch(cropIntent);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Crop function is not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }


    private final ActivityResultLauncher<Intent> cropImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK) {
                    ivProfilePicture.setImageURI(croppedImageUri);
                    savePictureToFirebaseStorage();
                }
            }
    );


    private void savePictureToFirebaseStorage() {
        if (croppedImageUri == null) return;

        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getCurrentUser().getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images/" + uid + ".jpg");

        storageRef.putFile(croppedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
                    sharedPreferences.edit().putString("profileImageUri", uri.toString()).apply();
                    ivProfilePicture.setImageURI(croppedImageUri);
                }))
                .addOnFailureListener(e -> Log.e("SaveImage", "Failed to upload image", e));
    }

    private void loadProfilePicture() {
        sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        String savedUriString = sharedPreferences.getString("profileImageUri", null);

        if (savedUriString != null) {
            ivProfilePicture.setImageURI(Uri.parse(savedUriString));
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
        if (croppedImageUri != null) {
            Intent intent = new Intent(getActivity(), ViewPhotoActivity.class);
            intent.putExtra("photoUri", croppedImageUri.toString());
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

    public void loadColor(Activity activity, View view){
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("color", R.color.Default);
        view.setBackgroundColor(ContextCompat.getColor(activity, color));
    }


}