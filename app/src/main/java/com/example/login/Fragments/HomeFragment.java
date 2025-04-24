package com.example.login.Fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private SharedPreferences sharedPreferences;
    private RecyclerView paintingsRecyclerView;
    private List<Painting> paintingList = new ArrayList<>();
    private PaintingAdapter adapter;
    private static final int REQUEST_CODE_VIEW_PAINTING = 1001;
    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate your fragment_home layout which must contain a RecyclerView with id "paintingsRecyclerView"
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        loadBgColor(getActivity(), view);

        paintingsRecyclerView = view.findViewById(R.id.paintingsRecyclerView);
        paintingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new PaintingAdapter();
        paintingsRecyclerView.setAdapter(adapter);

        loadPaintings();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPaintings();
    }

    private class PaintingViewHolder extends RecyclerView.ViewHolder {
        TextView paintingName, description, userName, creationDate, likeCount;
        ImageView paintingImage, userPhoto, likeButton;

        public PaintingViewHolder(@NonNull View itemView) {
            super(itemView);
            paintingName = itemView.findViewById(R.id.paintingName);
            description = itemView.findViewById(R.id.description);
            userName = itemView.findViewById(R.id.userName);
            creationDate = itemView.findViewById(R.id.creationDate);
            likeCount = itemView.findViewById(R.id.likeCount);
            paintingImage = itemView.findViewById(R.id.paintingImage);
            userPhoto = itemView.findViewById(R.id.userPhoto);
            likeButton = itemView.findViewById(R.id.likeButton);
        }
    }

    private class PaintingAdapter extends RecyclerView.Adapter<PaintingViewHolder> {
        @NonNull
        @Override
        public PaintingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_painting, parent, false);
            return new PaintingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final PaintingViewHolder holder, int position) {
            final Painting painting = paintingList.get(position);

            // Painting info
            holder.paintingName.setText(painting.getName());
            if (painting.getDescription() != null && !painting.getDescription().isEmpty()) {
                holder.description.setText(painting.getDescription());
                holder.description.setVisibility(View.VISIBLE);
            } else {
                holder.description.setVisibility(View.GONE);
            }
            Date date = new Date(painting.getCreationTime());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.creationDate.setText("Date of creation: " + sdf.format(date));
            holder.likeCount.setText(painting.getLikes() + " likes");

            // Load painting image
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
            // Click on painting image launches ViewPaintingActivity
            holder.paintingImage.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), ViewPaintingActivity.class);
                intent.putExtra("painting", painting);
                startActivityForResult(intent, REQUEST_CODE_VIEW_PAINTING);
            });

            // Author name and profile image
            if (painting.getIsAnonymous()) {
                holder.userName.setText("Anonymous");
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.default_profile)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .transform(new CircleCrop())
                        .into(holder.userPhoto);
            } else {
                if (painting.getAuthorName() != null && !painting.getAuthorName().isEmpty()) {
                    holder.userName.setText(painting.getAuthorName());
                } else {
                    holder.userName.setText("Unknown");
                }
                // Load user profile image from Storage using the uid.
                String uid = painting.getUid();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference().child("profile_images").child(uid + ".jpg");
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Glide.with(holder.itemView.getContext())
                            .load(uri)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .transform(new CircleCrop())
                            .into(holder.userPhoto);
                }).addOnFailureListener(e -> {
                    Glide.with(holder.itemView.getContext())
                            .load(R.drawable.default_profile)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .transform(new CircleCrop())
                            .into(holder.userPhoto);
                });
            }

            // Like button logic (unchanged from your previous code)
            final String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (painting.getLikedBy() != null && painting.getLikedBy().contains(currentUid)) {
                holder.likeButton.setImageResource(R.drawable.ic_like_on);
            } else {
                holder.likeButton.setImageResource(R.drawable.ic_like_off);
            }
            holder.likeButton.setOnClickListener(v -> {
                LoadingManagerDialog.showLoading(getActivity(), "Processing…");
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
                        holder.likeButton.setImageResource(R.drawable.ic_like_on);
                        holder.likeButton.animate()
                                .scaleX(1.3f).scaleY(1.3f)
                                .setDuration(150)
                                .withEndAction(() -> holder.likeButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                                .start();
                        painting.setLikes(painting.getLikes() + 1);
                        if (painting.getLikedBy() != null) {
                            painting.getLikedBy().add(currentUid);
                        }
                    } else if (result.equals("unliked")) {
                        holder.likeButton.setImageResource(R.drawable.ic_like_off);
                        holder.likeButton.animate()
                                .scaleX(0.7f).scaleY(0.7f)
                                .setDuration(150)
                                .withEndAction(() -> holder.likeButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                                .start();
                        painting.setLikes(painting.getLikes() - 1);
                        if (painting.getLikedBy() != null) {
                            painting.getLikedBy().remove(currentUid);
                        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_VIEW_PAINTING && resultCode == Activity.RESULT_OK) {
            // Refresh the paintings after deletion or any modification.
            loadPaintings();
        }
    }

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



    public void loadBgColor(Activity activity, View view) {
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("bgColor", (R.color.Default));
        view.setBackgroundColor(color);
    }
}
