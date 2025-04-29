package com.example.login.Activities;

import static android.text.TextUtils.TruncateAt.END;
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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.login.Classes.Painting;
import com.example.login.Dialogs.LoadingManagerDialog;
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

/**
 * Activity for viewing a painting in detail.
 * Displays the painting, metadata, and allows like, delete, and share actions.
 */
public class ViewPaintingActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivPainting;
    private ImageView ivLikeButton;
    private Button btnBack, btnDelete, btnShare;
    private TextView tvPaintingName, tvPaintingDate, tvPaintingDescription, tvPaintingLikes, tvAuthor, tvPaintingNameMore, tvPaintingDescriptionMore;
    private Painting painting; // Passed via Intent (Serializable)
    private View view;
    private ScrollView bottomPane;
    private Guideline guideline;
    SharedPreferences sp;

    /**
     * Initializes the activity, finds UI elements, and sets up interactions.
     * @param savedInstanceState saved state bundle
     */
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
        tvPaintingNameMore = findViewById(R.id.tvPaintingNameMore);
        tvPaintingDescriptionMore = findViewById(R.id.tvPaintingDescriptionMore);
        bottomPane = findViewById(R.id.bottomPane);
        guideline = findViewById(R.id.guideline);

        btnBack.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnShare.setOnClickListener(this);

        sp = this.getSharedPreferences("userDetails", Context.MODE_PRIVATE);

        setupExpandableText(tvPaintingName, tvPaintingNameMore);
        setupExpandableText(tvPaintingDescription, tvPaintingDescriptionMore);

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
        adjustGuideline();

        // make sure the ImageView is clickable
        ivPainting.setClickable(true);

        // gesture detector for double-tap like
        GestureDetector gestureDetector = new GestureDetector(
                this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        ivLikeButton.performClick();
                        return true;
                    }
                }
        );
        ivPainting.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    /**
     * Adjusts the bottom pane guideline based on content size.
     */
    private void adjustGuideline() {
        bottomPane.post(() -> {
            View content = bottomPane.getChildAt(0);
            int wSpec = View.MeasureSpec.makeMeasureSpec(bottomPane.getWidth(), View.MeasureSpec.AT_MOST);
            int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            content.measure(wSpec, hSpec);
            int contentH = content.getMeasuredHeight();
            int totalH = view.getHeight();
            float maxBot = totalH * 0.35f;
            if (contentH <= maxBot) {
                float topPct = 1f - ((float) contentH / totalH);
                topPct = Math.max(0.65f, Math.min(topPct, 1f));
                guideline.setGuidelinePercent(topPct);
            } else {
                guideline.setGuidelinePercent(0.65f);
            }
        });
    }

    /**
     * Configures expandable text behavior for name and description fields.
     */
    private void setupExpandableText(TextView contentTextView, TextView toggleTextView) {
        contentTextView.setMaxLines(2);

        // Set up click listener only once, outside the layout listener
        toggleTextView.setOnClickListener(v -> {
            if (contentTextView.getMaxLines() == 2) {
                contentTextView.setMaxLines(Integer.MAX_VALUE);
                toggleTextView.setText("Show less");
            } else {
                contentTextView.setMaxLines(2);
                toggleTextView.setText("Show more");
            }
            adjustGuideline();
        });

        contentTextView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (contentTextView.getLayout() != null) {
                boolean isEllipsized = contentTextView.getLayout().getEllipsisCount(1) > 0;
                int lineCount = contentTextView.getLineCount();
                if (isEllipsized || lineCount > 2) {
                    toggleTextView.setVisibility(View.VISIBLE);
                    // Don't set the text if we're expanded (maxLines > 2)
                    if (contentTextView.getMaxLines() <= 2) {
                        toggleTextView.setText("Show more");
                    }
                } else {
                    toggleTextView.setVisibility(View.GONE);
                }
                adjustGuideline();
            }
        });
    }
    /**
     * Loads painting details and displays them in the UI.
     */
    private void loadPaintingDetails() {
        painting = (Painting) getIntent().getSerializableExtra("painting");
        if (painting != null) {
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
            if (painting.getIsAnonymous()) {
                tvAuthor.setText("By: Anonymous");
            } else {
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
                                        ? firstName + " " + lastName : "Unknown";
                                tvAuthor.setText("By: "+fullName);
                                painting.setAuthorName(fullName);
                            } else {
                                tvAuthor.setText("Unknown");
                            }
                        })
                        .addOnFailureListener(e -> tvAuthor.setText("Unknown"));
            }
        } else {
            Toast.makeText(this, "No painting available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the painting image from a URL or URI into the ImageView.
     */
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

    /**
     * Sets up like button state and click handling with Firebase.
     */
    private void setupLikeButton() {
        if (ivLikeButton != null && painting != null) {
            final String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (painting.getLikedBy() != null && painting.getLikedBy().contains(currentUid)) {
                ivLikeButton.setImageResource(R.drawable.ic_like_on);
            } else {
                ivLikeButton.setImageResource(R.drawable.ic_like_off);
            }
            ivLikeButton.setOnClickListener(v -> {
                LoadingManagerDialog.showLoading(this, "Processing...");
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
                    LoadingManagerDialog.hideLoading();
                }).addOnFailureListener(e -> {
                    Toast.makeText(ViewPaintingActivity.this, "Failed to update like", Toast.LENGTH_SHORT).show();
                    LoadingManagerDialog.hideLoading();
                });
            });
        }
    }

    /**
     * Handles clicks on back, delete, and share buttons.
     * @param v the clicked view
     */
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

    /**
     * Prompts deletion confirmation and deletes painting and storage.
     */
    private void deletePainting() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Painting")
                .setMessage("Are you sure you want to delete this painting? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (painting == null || painting.getDocId() == null) {
                        Toast.makeText(this, "Painting cannot be deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    LoadingManagerDialog.showLoading(this, "Deleting painting...");
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
                                    LoadingManagerDialog.hideLoading();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to delete painting metadata", Toast.LENGTH_SHORT).show();
                                    LoadingManagerDialog.hideLoading();
                                });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete painting from storage", Toast.LENGTH_SHORT).show();
                        LoadingManagerDialog.hideLoading();
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Shares the painting image and details via an Android share intent.
     */
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
            if (!cachePath.exists()) cachePath.mkdirs();
            File file = new File(cachePath, "image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            String author = painting.getIsAnonymous() ? "Anonymous" : painting.getAuthorName();
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this painting!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this painting: " + painting.getName() +
                    "\nBy: " + author +
                    "\nDescription: " + painting.getDescription());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Painting"));
        } catch (Exception e) {
            Log.e("ViewPaintingActivity", "Failed to share painting: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to share painting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads background and button colors from SharedPreferences.
     */
    public void loadBgColor() {
        int color = sp.getInt("bgColor", R.color.Default);
        view.setBackgroundColor(color);
        loadBtnColor((ViewGroup) view);
    }

    /**
     * Recursively applies button tint to all buttons in the view hierarchy.
     * @param rootView the root view group
     */
    private void loadBtnColor(ViewGroup rootView) {
        int color = sp.getInt("btnColor", R.color.button);
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