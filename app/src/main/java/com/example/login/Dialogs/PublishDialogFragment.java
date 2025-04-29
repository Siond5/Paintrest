package com.example.login.Dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

/**
 * A DialogFragment for publishing a painting.
 * <p>
 * Shows a preview of the provided Bitmap, allows the user to enter a name and description,
 * choose whether to publish anonymously, and then uploads the image to Firebase Storage
 * and its metadata (including author, timestamp, likes, and anonymity flag) to Firestore.
 * Also applies a button tint color loaded from SharedPreferences.
 */
public class PublishDialogFragment extends DialogFragment {

    private Bitmap paintingBitmap;
    private ImageView imagePreview;
    private EditText edtPaintingName;
    private EditText edtPaintingDescription;
    private Button btnPublish;
    private Button btnCancel;
    private CheckBox isAnonymous;
    private boolean IsAnonymous;

    /**
     * Factory method to create a new instance of PublishDialogFragment
     * with the given painting Bitmap.
     *
     * @param bitmap The Bitmap of the painting to preview and publish.
     * @return A new instance of PublishDialogFragment.
     */
    public static PublishDialogFragment newInstance(Bitmap bitmap) {
        PublishDialogFragment fragment = new PublishDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("paintingBitmap", bitmap);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to do initial creation of the fragment.
     * Retrieves the painting Bitmap from arguments if provided.
     *
     * @param savedInstanceState The saved state bundle, if any.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the bitmap passed as an argument
        if (getArguments() != null) {
            paintingBitmap = getArguments().getParcelable("paintingBitmap");
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, binds views, sets up listeners, and applies stored button colors.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate views.
     * @param container          The parent view that the fragment’s UI should be attached to.
     * @param savedInstanceState The saved state bundle, if any.
     * @return The root View of the fragment’s layout.
     */
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
        isAnonymous = view.findViewById(R.id.isAnonymous);

        // Set the preview image
        if (paintingBitmap != null) {
            imagePreview.setImageBitmap(paintingBitmap);
        }

        btnPublish.setOnClickListener(v -> publishPainting());
        btnCancel.setOnClickListener(v -> dismiss());
        edtPaintingName.addTextChangedListener(createValidationWatcher());
        loadBtnColor((ViewGroup) view);

        return view;
    }

    /**
     * Creates a TextWatcher that validates the painting name field on text change.
     * Displays an error if the name is empty.
     *
     * @return A configured TextWatcher instance.
     */
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

    /**
     * Handles the publish action:
     * - Validates input
     * - Compresses the bitmap to PNG
     * - Uploads the image bytes to Firebase Storage
     * - Retrieves the download URL
     * - Calls saveMetadata() to store painting data in Firestore
     */
    private void publishPainting() {
        LoadingManagerDialog.showLoading(getActivity(), "Publishing painting...");

        IsAnonymous = this.isAnonymous.isChecked();
        final String paintingName = edtPaintingName.getText().toString().trim();
        final String paintingDescription = edtPaintingDescription.getText().toString().trim();

        if (paintingName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter the painting's name.", Toast.LENGTH_SHORT).show();
            LoadingManagerDialog.hideLoading();
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
                            LoadingManagerDialog.hideLoading();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to retrieve image URL", Toast.LENGTH_SHORT).show();
                            LoadingManagerDialog.hideLoading();
                        })
                )
                .addOnFailureListener(e -> {
                    LoadingManagerDialog.hideLoading();
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Saves painting metadata to Firestore.
     *
     * @param name        The painting name entered by the user.
     * @param description The painting description entered by the user.
     * @param imageUrl    The download URL of the uploaded image.
     */
    private void saveMetadata(String name, String description, String imageUrl) {
        Map<String, Object> paintingData = new HashMap<>();
        paintingData.put("name", name);
        paintingData.put("description", description);
        paintingData.put("uid", FirebaseAuth.getInstance().getUid());
        paintingData.put("date", Timestamp.now());
        paintingData.put("likes", 0);
        paintingData.put("imageUrl", imageUrl);
        paintingData.put("isAnonymous", IsAnonymous);

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

    /**
     * Recursively loads a stored button tint color from SharedPreferences
     * and applies it to all Button views within the provided root view group.
     *
     * @param rootView The root ViewGroup whose Button children will be tinted.
     */
    private void loadBtnColor(ViewGroup rootView) {
        SharedPreferences sharedPreferences =
                getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("btnColor", R.color.button);
        ColorStateList buttonColor = ColorStateList.valueOf(color);

        for (int i = 0; i < rootView.getChildCount(); i++) {
            View childView = rootView.getChildAt(i);

            if (childView instanceof Button) {
                ((Button) childView).setBackgroundTintList(buttonColor);
            } else if (childView instanceof ViewGroup) {
                loadBtnColor((ViewGroup) childView);
            }
        }
    }
}