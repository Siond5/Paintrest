package com.example.login.Activities;

import static android.view.View.GONE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.login.Classes.Painting;
import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

public class ViewPaintingActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivPainting;
    private ImageView ivLikeButton;
    private Button btnBack, btnDelete, btnShare;
    private TextView tvPaintingName, tvPaintingDate, tvPaintingDescription, tvPaintingLikes, tvAuthor;
    private Painting painting; // Passed via Intent (Serializable)
    private View view;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_painting);

        // Find UI elements.
        view = findViewById(R.id.main);
        ivPainting = findViewById(R.id.ivPainting);
        ivLikeButton = findViewById(R.id.ivLikeButton);
        tvPaintingName = findViewById(R.id.tvPaintingName);
        tvPaintingDate = findViewById(R.id.tvPaintingDate);
        tvPaintingDescription = findViewById(R.id.tvPaintingDescription);
        tvPaintingLikes = findViewById(R.id.tvPaintingLikes);
        tvAuthor = findViewById(R.id.tvAuthor);
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);
        btnShare = findViewById(R.id.btn_share);
        btnBack.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnShare.setOnClickListener(this);

        sp = this.getSharedPreferences("userDetails", Context.MODE_PRIVATE);

        loadPaintingDetails();

        // Show the Delete button only if the current user is the owner.
        if (painting != null && painting.getUid() != null) {
            String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (painting.getUid().equals(currentUid)) {
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                btnDelete.setVisibility(GONE);
            }
        }

        // Setup the like button.
        setupLikeButton();
        loadBgColor();
    }

    private void loadPaintingDetails() {
        painting = (Painting) getIntent().getSerializableExtra("painting");
        if (painting != null) {
            // Load the painting image.
            loadPainting(painting.getImageUrl());

            tvPaintingName.setText(painting.getName());
            String formattedDate = DateFormat.format("MMM dd, yyyy hh:mm a", new Date(painting.getCreationTime())).toString();
            tvPaintingDate.setText(formattedDate);

            if (painting.getDescription() != null && !painting.getDescription().isEmpty()) {
                tvPaintingDescription.setText(painting.getDescription());
            } else {
                tvPaintingDescription.setVisibility(GONE);
            }

            tvPaintingLikes.setText("Likes: " + painting.getLikes());

            // For the author details:
            if (painting.getIsAnonymous()) {
                tvAuthor.setText("By: Anonymous");
            }
                 else {
                    // Fetch from Firestore as a fallback.
                    String uid = painting.getUid();
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String firstName = documentSnapshot.getString("firstName");
                                    String lastName = documentSnapshot.getString("lastName");
                                    String fullName = (firstName != null && lastName != null)
                                            ?"By: " + firstName + " " + lastName : "Unknown";
                                    tvAuthor.setText(fullName);
                                    // Optionally, update the painting's authorName for future use.
                                    painting.setAuthorName(fullName);
                                } else {
                                    tvAuthor.setText("Unknown");
                                }
                            })
                            .addOnFailureListener(e -> tvAuthor.setText("Unknown"));
                }
            }
         else {
            Toast.makeText(this, "No painting available", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPainting(String imageUrl) {
        if (imageUrl == null) {
            Toast.makeText(this, "No painting available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(imageUrl);
            if (uri.getScheme() != null && (uri.getScheme().startsWith("http") || uri.getScheme().startsWith("https"))) {
                Glide.with(this)
                        .load(uri)
                        .into(ivPainting);
            } else {
                ivPainting.setImageURI(uri);
            }
        } catch (Exception e) {
            Log.e("ViewPaintingActivity", "Failed to load painting", e);
            Toast.makeText(this, "Failed to load painting", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLikeButton() {
        // Ensure ivLikeButton is not null and painting is loaded.
        if (ivLikeButton != null && painting != null) {
            final String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Set initial like state.
            if (painting.getLikedBy() != null && painting.getLikedBy().contains(currentUid)) {
                ivLikeButton.setImageResource(R.drawable.ic_like_on);
            } else {
                ivLikeButton.setImageResource(R.drawable.ic_like_off);
            }

            ivLikeButton.setOnClickListener(v -> {
                DocumentReference docRef = FirebaseFirestore.getInstance()
                        .collection("paintings")
                        .document(painting.getDocId());

                FirebaseFirestore.getInstance().runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef);
                    List<String> likedBy = (List<String>) snapshot.get("likedBy");
                    if (likedBy != null && likedBy.contains(currentUid)) {
                        transaction.update(docRef, "likedBy", FieldValue.arrayRemove(currentUid));
                        transaction.update(docRef, "likes", FieldValue.increment(-1));
                        return "unliked";
                    } else {
                        transaction.update(docRef, "likedBy", FieldValue.arrayUnion(currentUid));
                        transaction.update(docRef, "likes", FieldValue.increment(1));
                        return "liked";
                    }
                }).addOnSuccessListener(result -> {
                    if (result.equals("liked")) {
                        ivLikeButton.setImageResource(R.drawable.ic_like_on);
                        ivLikeButton.animate()
                                .scaleX(1.3f).scaleY(1.3f)
                                .setDuration(150)
                                .withEndAction(() -> ivLikeButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                                .start();
                        painting.setLikes(painting.getLikes() + 1);
                        if (painting.getLikedBy() != null) {
                            painting.getLikedBy().add(currentUid);
                        }
                    } else if (result.equals("unliked")) {
                        ivLikeButton.setImageResource(R.drawable.ic_like_off);
                        ivLikeButton.animate()
                                .scaleX(0.7f).scaleY(0.7f)
                                .setDuration(150)
                                .withEndAction(() -> ivLikeButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                                .start();
                        painting.setLikes(painting.getLikes() - 1);
                        if (painting.getLikedBy() != null) {
                            painting.getLikedBy().remove(currentUid);
                        }
                    }
                    tvPaintingLikes.setText("Likes: " + painting.getLikes());
                }).addOnFailureListener(e ->
                        Toast.makeText(ViewPaintingActivity.this, "Failed to update like", Toast.LENGTH_SHORT).show());
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnDelete) {
            deletePainting();
        } else if (v == btnShare) {
            sharePainting();
        }
    }

    private void deletePainting() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Painting")
                .setMessage("Are you sure you want to delete this painting? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (painting == null || painting.getDocId() == null) {
                        Toast.makeText(this, "Painting cannot be deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Delete the painting image from Firebase Storage.
                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(painting.getImageUrl());
                    imageRef.delete().addOnSuccessListener(aVoid -> {
                        FirebaseFirestore.getInstance()
                                .collection("paintings")
                                .document(painting.getDocId())
                                .delete()
                                .addOnSuccessListener(documentVoid -> {
                                    Toast.makeText(this, "Painting deleted successfully", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete painting metadata", Toast.LENGTH_SHORT).show());
                    }).addOnFailureListener(e -> Toast.makeText(this, "Failed to delete painting from storage", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void sharePainting() {
        if (painting == null || painting.getImageUrl() == null) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ivPainting.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(ivPainting.getDrawingCache());
            ivPainting.setDrawingCacheEnabled(false);

            File cachePath = new File(getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            File file = new File(cachePath, "image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            String author = painting.getIsAnonymous() ? "Anonymous" :
                    (painting.getAuthorName() != null && !painting.getAuthorName().isEmpty() ? painting.getAuthorName() : "Unknown");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this painting!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this painting: " + painting.getName() +
                    "\nBy: " + author +
                    "\nDescription: " + painting.getDescription());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Painting"));

        } catch (Exception e) {
            Log.e("ViewPaintingActivity", "Failed to share painting", e);
            Toast.makeText(this, "Failed to share painting", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadBgColor() {
        int color = sp.getInt("bgColor", R.color.Default);
        view.setBackgroundColor(color);
        loadBtnColor((ViewGroup) view);
    }

    private void loadBtnColor(ViewGroup rootView) {
        int color = sp.getInt("btnColor", R.color.button);
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
