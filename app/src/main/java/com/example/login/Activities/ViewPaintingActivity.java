package com.example.login.Activities;

import static android.view.View.GONE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.login.Classes.Painting; // Your updated Painting model with extra fields
import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

public class ViewPaintingActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivPainting;
    private Button btnBack, btnDelete, btnShare;
    private TextView tvPaintingName, tvPaintingDate, tvPaintingDescription, tvPaintingLikes;
    private Painting painting; // Received via Intent as Serializable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_painting);
        // Edge-to-edge: apply system insets to the root view with id "main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find UI elements
        ivPainting = findViewById(R.id.ivPainting);
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);
        btnShare = findViewById(R.id.btn_share);
        tvPaintingName = findViewById(R.id.tvPaintingName);
        tvPaintingDate = findViewById(R.id.tvPaintingDate);
        tvPaintingDescription = findViewById(R.id.tvPaintingDescription);
        tvPaintingLikes = findViewById(R.id.tvPaintingLikes);

        btnBack.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnShare.setOnClickListener(this);

        // Retrieve the Painting object from the Intent extra (must implement Serializable)
        loadPaintingDetails();

        // Disable delete button if no document ID is provided.
        if (painting == null || painting.getDocId() == null) {
            btnDelete.setEnabled(false);
            btnDelete.setAlpha(0.5f);
        }
    }

    private void loadPaintingDetails() {
        painting = (Painting) getIntent().getSerializableExtra("painting");
        if (painting != null) {
            loadPainting(painting.getImageUrl());
            tvPaintingName.setText(painting.getName());
            String formattedDate = DateFormat.format("MMM dd, yyyy hh:mm a", new Date(painting.getCreationTime())).toString();
            tvPaintingDate.setText(formattedDate);
            if (!painting.getDescription().equals("")) {
                tvPaintingDescription.setText(painting.getDescription());
            } else {
                tvPaintingDescription.setVisibility(GONE);
            }
            tvPaintingLikes.setText("Likes: " + painting.getLikes());
        } else {
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
                .setMessage("Are you sure you want to delete your painting? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (painting == null || painting.getDocId() == null) {
                        Toast.makeText(this, "Painting cannot be deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(painting.getImageUrl());
                    imageRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                FirebaseFirestore.getInstance()
                                        .collection("paintings")
                                        .document(painting.getDocId())
                                        .delete()
                                        .addOnSuccessListener(documentVoid -> {
                                            Toast.makeText(this, "Painting deleted successfully", Toast.LENGTH_SHORT).show();
                                            setResult(RESULT_OK);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to delete painting metadata", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete painting from storage", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void sharePainting() {
        if (painting == null || painting.getImageUrl() == null) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Get the bitmap from the ImageView
            ivPainting.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(ivPainting.getDrawingCache());
            ivPainting.setDrawingCacheEnabled(false);

            // Save the bitmap to a cache file
            File cachePath = new File(getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            File file = new File(cachePath, "image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

            // Get content URI for sharing using FileProvider
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            // Create the share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            SharedPreferences sharedPreferences = getSharedPreferences("userDetails", MODE_PRIVATE);
            String author = sharedPreferences.getString("firstName", null) + " " + sharedPreferences.getString("lastName", null);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this painting!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this painting: " + painting.getName() + "\nBy: " + author + "\nDescription: " + painting.getDescription());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Painting"));

        } catch (Exception e) {
            Log.e("ViewPaintingActivity", "Failed to share painting", e);
            Toast.makeText(this, "Failed to share painting", Toast.LENGTH_SHORT).show();
        }
    }
}
