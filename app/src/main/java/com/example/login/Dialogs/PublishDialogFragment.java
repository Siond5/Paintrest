package com.example.login.Dialogs;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.login.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class PublishDialogFragment extends DialogFragment {

    private Bitmap paintingBitmap;
    private ImageView imagePreview;
    private EditText edtPaintingName;
    private EditText edtPaintingDescription;
    private Button btnPublish;
    private Button btnCancel;

    // Factory method to create a new instance passing the Bitmap
    public static PublishDialogFragment newInstance(Bitmap bitmap) {
        PublishDialogFragment fragment = new PublishDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("paintingBitmap", bitmap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the bitmap passed as an argument
        if (getArguments() != null) {
            paintingBitmap = getArguments().getParcelable("paintingBitmap");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.publish_dialog, container, false);

        imagePreview = view.findViewById(R.id.image_preview);
        edtPaintingName = view.findViewById(R.id.edt_painting_name);
        edtPaintingDescription = view.findViewById(R.id.edt_painting_description);
        btnPublish = view.findViewById(R.id.btn_publish);
        btnCancel = view.findViewById(R.id.btn_cancel);

        // Set the preview image
        if (paintingBitmap != null) {
            imagePreview.setImageBitmap(paintingBitmap);
        }

        btnPublish.setOnClickListener(v -> publishPainting());
        btnCancel.setOnClickListener(v -> dismiss());
        edtPaintingName.addTextChangedListener(createValidationWatcher());
        return view;
    }

    private TextWatcher createValidationWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String name = edtPaintingName.getText().toString();
                if (name.isEmpty()) {
                    edtPaintingName.setError("Please enter the painting's name.");
                } else {
                    edtPaintingName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };
    }

    private void publishPainting() {
        final String paintingName = edtPaintingName.getText().toString().trim();
        final String paintingDescription = edtPaintingDescription.getText().toString().trim();

        if (paintingName.isEmpty()){
            Toast.makeText(getContext(), "Please enter the painting's name.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        paintingBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        btnPublish.setClickable(false);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String filename = "paint_" + System.currentTimeMillis() + ".png";
        StorageReference imageRef = storageRef.child("paintings")
                .child(FirebaseAuth.getInstance().getUid())
                .child(filename);

        imageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            saveMetadata(paintingName, paintingDescription, downloadUrl);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to retrieve image URL", Toast.LENGTH_SHORT).show();
                        })
                )
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMetadata(String name, String description, String imageUrl) {
        // Build a map of painting metadata
        Map<String, Object> paintingData = new HashMap<>();
        paintingData.put("name", name);
        paintingData.put("description", description);
        paintingData.put("uid", FirebaseAuth.getInstance().getUid());
        paintingData.put("date", Timestamp.now());
        paintingData.put("likes", 0);
        paintingData.put("imageUrl", imageUrl);

        FirebaseFirestore.getInstance().collection("paintings")
                .add(paintingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Painting published successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save painting data", Toast.LENGTH_SHORT).show();
                });
    }
}
