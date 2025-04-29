package com.example.login.Fragments;

import static android.text.TextUtils.TruncateAt.END;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.login.Activities.ViewPaintingActivity;
import com.example.login.Classes.Painting;
import com.example.login.Dialogs.LoadingManagerDialog;
import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment displaying a scrollable list of paintings from Firestore.
 * <p>
 * Supports viewing details, liking/unliking, expandable text fields,
 * and handles anonymous posts and profile image loading.
 * </p>
 */
public class HomeFragment extends Fragment {

    /** SharedPreferences for storing user-specific settings like background color. */
    private SharedPreferences sharedPreferences;

    /** RecyclerView displaying painting items. */
    private RecyclerView paintingsRecyclerView;

    /** List backing the RecyclerView adapter. */
    private List<Painting> paintingList = new ArrayList<>();

    /** Adapter for painting items. */
    private PaintingAdapter adapter;

    /** Request code for launching ViewPaintingActivity. */
    private static final int REQUEST_CODE_VIEW_PAINTING = 1001;

    /**
     * Required empty public constructor.
     */
    public HomeFragment() {
    }

    /**
     * Inflates the fragment layout, initializes RecyclerView and loads paintings.
     *
     * @param inflater           LayoutInflater to inflate views.
     * @param container          Parent view that the fragment UI should attach to.
     * @param savedInstanceState Saved state bundle.
     * @return Root view of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        loadBgColor(getActivity(), view);

        paintingsRecyclerView = view.findViewById(R.id.paintingsRecyclerView);
        paintingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new PaintingAdapter();
        paintingsRecyclerView.setAdapter(adapter);

        loadPaintings();

        return view;
    }

    /**
     * Reloads paintings when fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadPaintings();
    }

    /**
     * ViewHolder for painting items, caching subviews.
     */
    private class PaintingViewHolder extends RecyclerView.ViewHolder {
        TextView paintingName, description, userName, creationDate, likeCount, paintingNameMore, descriptionMore;
        ImageView paintingImage, userPhoto, likeButton;

        /**
         * Constructs a ViewHolder and finds child views.
         *
         * @param itemView The inflated item view.
         */
        public PaintingViewHolder(@NonNull View itemView) {
            super(itemView);
            paintingName = itemView.findViewById(R.id.paintingName);
            paintingNameMore = itemView.findViewById(R.id.paintingNameMore);
            description = itemView.findViewById(R.id.description);
            descriptionMore = itemView.findViewById(R.id.descriptionMore);
            userName = itemView.findViewById(R.id.userName);
            creationDate = itemView.findViewById(R.id.creationDate);
            likeCount = itemView.findViewById(R.id.likeCount);
            paintingImage = itemView.findViewById(R.id.paintingImage);
            userPhoto = itemView.findViewById(R.id.userPhoto);
            likeButton = itemView.findViewById(R.id.likeButton);
        }
    }

    /**
     * RecyclerView Adapter for painting list.
     */
    private class PaintingAdapter extends RecyclerView.Adapter<PaintingViewHolder> {

        @NonNull
        @Override
        public PaintingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_painting, parent, false);
            return new PaintingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final PaintingViewHolder holder, int position) {
            final Painting painting = paintingList.get(position);

            // Set painting name and expandable toggle
            holder.paintingName.setText(painting.getName());
            setupExpandableText(holder.paintingName, holder.paintingNameMore);

            // Description handling
            if (painting.getDescription() != null && !painting.getDescription().isEmpty()) {
                holder.description.setText(painting.getDescription());
                holder.description.setVisibility(View.VISIBLE);
                setupExpandableText(holder.description, holder.descriptionMore);
            } else {
                holder.description.setVisibility(View.GONE);
                holder.descriptionMore.setVisibility(View.GONE);
            }

            // Date display
            Date date = new Date(painting.getCreationTime());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.creationDate.setText("Date of creation: " + sdf.format(date));

            // Like count
            holder.likeCount.setText(painting.getLikes() + " likes");

            // Load painting image with placeholder/error
            String imageUrl = painting.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_error_24)
                        .error(R.drawable.ic_error_24)
                        .into(holder.paintingImage);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.ic_error_24)
                        .into(holder.paintingImage);
            }

            // Gesture detection for single and double tap
            GestureDetector gestureDetector = new GestureDetector(
                    holder.itemView.getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent e) {
                            return true;
                        }

                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            Intent intent = new Intent(holder.itemView.getContext(), ViewPaintingActivity.class);
                            intent.putExtra("painting", painting);
                            startActivityForResult(intent, REQUEST_CODE_VIEW_PAINTING);
                            return true;
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            holder.likeButton.performClick();
                            return true;
                        }
                    }
            );
            holder.paintingImage.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });

            // Author info and profile image loading
            if (painting.getIsAnonymous()) {
                holder.userName.setText("Anonymous");
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.default_profile)
                        .transform(new CircleCrop())
                        .into(holder.userPhoto);
            } else {
                String author = painting.getAuthorName();
                holder.userName.setText(author != null && !author.isEmpty() ? author : "Unknown");
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReference().child("profile_images").child(painting.getUid() + ".jpg");
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(holder.itemView.getContext())
                                .load(uri).transform(new CircleCrop()).into(holder.userPhoto))
                        .addOnFailureListener(e -> Glide.with(holder.itemView.getContext())
                                .load(R.drawable.default_profile).transform(new CircleCrop()).into(holder.userPhoto));
            }

            // Like button state and click behavior
            final String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            holder.likeButton.setImageResource(
                    painting.getLikedBy() != null && painting.getLikedBy().contains(currentUid)
                            ? R.drawable.ic_like_on : R.drawable.ic_like_off
            );
            holder.likeButton.setOnClickListener(v -> {
                LoadingManagerDialog.showLoading(getActivity(), "Processing…");
                DocumentReference docRef = FirebaseFirestore.getInstance()
                        .collection("paintings").document(painting.getDocId());
                FirebaseFirestore.getInstance().runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef);
                    List<String> likedBy = (List<String>) snapshot.get("likedBy");
                    boolean liked = likedBy != null && likedBy.contains(currentUid);
                    transaction.update(docRef, "likedBy", liked
                            ? FieldValue.arrayRemove(currentUid)
                            : FieldValue.arrayUnion(currentUid));
                    transaction.update(docRef, "likes", FieldValue.increment(liked ? -1 : 1));
                    return liked ? "unliked" : "liked";
                }).addOnSuccessListener(result -> {
                    boolean nowLiked = "liked".equals(result);
                    holder.likeButton.setImageResource(nowLiked ? R.drawable.ic_like_on : R.drawable.ic_like_off);
                    holder.likeButton.animate()
                            .scaleX(nowLiked ? 1.3f : 0.7f)
                            .scaleY(nowLiked ? 1.3f : 0.7f)
                            .setDuration(150)
                            .withEndAction(() -> holder.likeButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                            .start();
                    painting.setLikes(painting.getLikes() + (nowLiked ? 1 : -1));
                    if (painting.getLikedBy() != null) {
                        if (nowLiked) painting.getLikedBy().add(currentUid);
                        else painting.getLikedBy().remove(currentUid);
                    }
                    holder.likeCount.setText(painting.getLikes() + " likes");
                    LoadingManagerDialog.hideLoading();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update like", Toast.LENGTH_SHORT).show();
                    LoadingManagerDialog.hideLoading();
                });
            });
        }

        @Override
        public int getItemCount() {
            return paintingList.size();
        }
    }

    /**
     * Handles result from ViewPaintingActivity to refresh list on changes.
     *
     * @param requestCode Request code supplied.
     * @param resultCode  Result code returned.
     * @param data        Intent containing result data.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VIEW_PAINTING && resultCode == Activity.RESULT_OK) {
            loadPaintings();
        }
    }

    /**
     * Configures a TextView to expand/collapse long text with a toggle.
     *
     * @param contentTextView Main text view.
     * @param toggleTextView  Toggle link view.
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
            }
        });
    }
    /**
     * Loads painting data from Firestore, builds Painting objects, and updates adapter.
     */
    private void loadPaintings() {
        LoadingManagerDialog.showLoading(getActivity(), "Loading paintings…");

        FirebaseFirestore.getInstance()
                .collection("paintings")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    paintingList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String docId = doc.getId();
                        String uid = doc.getString("uid");
                        String imageUrl = doc.getString("imageUrl");
                        String name = doc.getString("name");
                        long creationTime = doc.getTimestamp("date") != null
                                ? doc.getTimestamp("date").toDate().getTime()
                                : 0;
                        int likes = doc.getLong("likes") != null
                                ? doc.getLong("likes").intValue()
                                : 0;
                        String description = doc.getString("description");
                        Boolean isAnonymous = doc.getBoolean("isAnonymous");
                        List<String> likedBy = (List<String>) doc.get("likedBy");

                        Painting painting = new Painting(
                                imageUrl, docId, uid, name, description,
                                creationTime, likes,
                                (isAnonymous != null) ? isAnonymous : false,
                                ""
                        );
                        painting.setLikedBy(likedBy);

                        // Fetch author name asynchronously
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(userSnap -> {
                                    if (userSnap.exists()) {
                                        String first = userSnap.getString("firstName");
                                        String last  = userSnap.getString("lastName");
                                        painting.setAuthorName(
                                                (first != null && last != null)
                                                        ? first + " " + last
                                                        : "Unknown"
                                        );
                                    } else {
                                        painting.setAuthorName("Unknown");
                                    }
                                    adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    painting.setAuthorName("Unknown");
                                    adapter.notifyDataSetChanged();
                                });

                        paintingList.add(painting);
                    }
                    adapter.notifyDataSetChanged();
                    LoadingManagerDialog.hideLoading();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load paintings", Toast.LENGTH_SHORT).show();
                    LoadingManagerDialog.hideLoading();
                });
    }

    /**
     * Loads background color from SharedPreferences and applies to root view.
     *
     * @param activity Activity context for SharedPreferences.
     * @param view     Root view to apply background color.
     */
    public void loadBgColor(Activity activity, View view) {
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("bgColor", (R.color.Default));
        view.setBackgroundColor(color);
    }
}