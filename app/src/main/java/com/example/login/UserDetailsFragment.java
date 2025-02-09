package com.example.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.ByteArrayOutputStream;
import java.io.File;


public class UserDetailsFragment extends Fragment implements View.OnClickListener {
    private EditText etDetailsEmail, etDetailsFirstName, etDetailsLastName, etDetailsPhone, etDetailsYOB;
    private Button btnDetailsSave, btnLogout, btnDeleteAccount;
    private ImageView ivProfilePicture;
    private Uri profileImageUri;
    private String base64Photo;
    private SharedPreferences sharedPreferences;
    ActivityResultLauncher<Intent> pickPhotoFromGallery;

    public UserDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickPhotoFromGallery = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        profileImageUri = result.getData().getData();
                        ivProfilePicture.setImageURI(profileImageUri);
//                        savePictureToSharedPreferencesAndFirebase();
                    } else {
                        Toast.makeText(getActivity(), "Failed to select photo.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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
//        loadPictureFromSharedPreferencesAndFirebase();

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
        }

        else if (view == btnLogout) {
            Intent intent = new Intent(this.getActivity(), MainActivity.class);
            startActivity(intent);
            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            fbAuth.signOut();
            getActivity().finish();
        }

        else if (view == btnDeleteAccount) {
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

    private void showImageOptions() {
        String[] options = {"View Profile Picture", "Change Profile Picture", "Delete Profile Picture", "Cancel"};
        new AlertDialog.Builder(getContext())
                .setTitle("Set Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewPhoto();
                    } else if (which == 1) {
                        pickImage();
                    }
                    else if (which == 2) {
                        deletePhoto();
                    }
                    else if (which == 3) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void deletePhoto() {
        ivProfilePicture.setImageResource(R.drawable.default_profile);

            sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String base64Photo = sharedPreferences.getString("profileImage", null);
            editor.remove("profileImage");
            editor.remove("profileImageUri");
            editor.apply();
         if (base64Photo==null){
            Toast.makeText(getActivity(), "No profile photo to delete", Toast.LENGTH_SHORT).show();
        }

    }

    private void pickImage() {
        String[] options = {"Take a photo", "Pick from gallery"};
        new AlertDialog.Builder(getContext())
                .setTitle("Change Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Camera option
                        initializeCameraUri();
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri);
                        takePhoto.launch(intent);
                    } else if (which == 1) { // Gallery option
                        pickImageFromGallery();
                    }
                })
                .show();
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

    private ActivityResultLauncher<Intent> takePhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (profileImageUri != null) {
                            ivProfilePicture.setImageURI(profileImageUri);
//                            savePictureToSharedPreferencesAndFirebase();
                        } else {
                            Toast.makeText(getActivity(), "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "An error occurred. Please try again later.", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );



//    private void savePictureToSharedPreferencesAndFirebase ()
//    {
//        byte[] photoBytes = convertImageToByteArray(profileImageUri);
//        String base64Photo = Base64.encodeToString(photoBytes, Base64.DEFAULT);
//        if (profileImageUri!=null)
//        Toast.makeText(getActivity(), "hello", Toast.LENGTH_SHORT).show();
//        sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("profileImage", base64Photo);
//        editor.putString("profileImageUri", profileImageUri.toString());
//        editor.apply();
//    try {
//        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
//        String uid = fbAuth.getUid();
//        FirebaseFirestore store = FirebaseFirestore.getInstance();
//        Image profilePhoto = new Image(base64Photo, profileImageUri.toString());
//        store.collection("images").document(uid).set(profilePhoto).addOnSuccessListener(new OnSuccessListener<Void>() {
//            @Override
//            public void onSuccess(Void unused) {
//                Toast.makeText(getActivity(), "Photo saved", Toast.LENGTH_SHORT).show();
//            }
//        });
//}
//    catch (Exception e)
//    {
//       Toast.makeText(getActivity(), "Failed to save photo", Toast.LENGTH_SHORT).show();
//    }
//
//    }

//    private void loadPictureFromSharedPreferencesAndFirebase ()
//    {
//
//        sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
//         base64Photo = sharedPreferences.getString("profileImage", null);
//         profileImageUri = Uri.parse(sharedPreferences.getString("profileImageUri", null));
//        if (base64Photo == null) {
//            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
//            String uid = fbAuth.getUid();
//            FirebaseFirestore store = FirebaseFirestore.getInstance();
//            store.collection("images").document(uid).get().addOnSuccessListener(documentSnapshot -> {
//                Image profilePhoto = documentSnapshot.toObject(Image.class);
//            if (profilePhoto.getProfileImage() != null && profilePhoto.getProfileImageUri() != null) {
//                base64Photo = profilePhoto.getProfileImage();
//                profileImageUri = Uri.parse(profilePhoto.getProfileImageUri());
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putString("profileImage", base64Photo);
//                editor.putString("profileImageUri", profileImageUri.toString());
//                editor.apply();
//            }
//            });
//        }
//        byte[] photoBytes = Base64.decode(base64Photo, Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
//        ivProfilePicture.setImageBitmap(bitmap);
//
//    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoFromGallery.launch(intent);
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

}