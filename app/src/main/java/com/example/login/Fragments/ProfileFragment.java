package com.example.login.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.login.Activities.ViewPaintingActivity;
import com.example.login.Classes.Painting; // Use updated Painting class
import com.example.login.Views.PaintingItemView;
import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_CODE_VIEW_PAINTING = 1001; // NEW: Request code for launching ViewPaintingActivity

    private SharedPreferences sharedPreferences;
    private TextView numOfPaintings, FullName, Description;
    private ImageView profileImage;
    private RecyclerView paintingsRecyclerView;
    private TextView emptyPlaceholder;

    private List<Painting> paintings = new ArrayList<>();
    private PaintingItemView adapter;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        loadColor(getActivity(), view);

        numOfPaintings = view.findViewById(R.id.numOfPaintings);
        paintingsRecyclerView = view.findViewById(R.id.paintingsRecyclerView);
        emptyPlaceholder = view.findViewById(R.id.emptyPlaceholder);
        FullName = view.findViewById(R.id.FullName);
        Description = view.findViewById(R.id.Description);
        profileImage = view.findViewById(R.id.profileImage);

        loadUserDetails();

        // Setup RecyclerView with a GridLayoutManager (3 columns)
        paintingsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // Create adapter with click listener to open ViewPaintingActivity
        adapter = new PaintingItemView(paintings, painting -> {
            Intent intent = new Intent(getContext(), ViewPaintingActivity.class);
            intent.putExtra("painting", painting); // Pass the entire Painting object
            startActivityForResult(intent, REQUEST_CODE_VIEW_PAINTING);
        });
        paintingsRecyclerView.setAdapter(adapter);

        loadUserPaintings();
        return view;
    }

    private void loadUserDetails() {
        try {
            sharedPreferences = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
            String firstName = sharedPreferences.getString("firstName", null);
            String lastName = sharedPreferences.getString("lastName", null);
            String savedUriString = sharedPreferences.getString("profileImageUri", null);

            if (firstName != null && lastName != null) {

                FullName.setText(firstName + " " + lastName);
            } else {
                Toast.makeText(getActivity(), "Failed to load details. Please try again later.", Toast.LENGTH_SHORT).show();
            }

            if (savedUriString != null) {
                Glide.with(this)
                        .load(Uri.parse(savedUriString))
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .transform(new CircleCrop())
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.default_profile);
            }

        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to load details. Please try again later.", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_VIEW_PAINTING && resultCode == Activity.RESULT_OK) {
            // Refresh the paintings after deletion or any modification.
            loadUserPaintings();
        }
    }

    // Loads paintings from Firestore and builds full Painting objects.
    private void loadUserPaintings() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("paintings")
                .whereEqualTo("uid", currentUid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    paintings.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String url = doc.getString("imageUrl");
                        String name = doc.getString("name");
                        String description = doc.getString("description");
                        long creationTime = doc.getTimestamp("date") != null ?
                                doc.getTimestamp("date").toDate().getTime() : 0;
                        int likes = doc.getLong("likes") != null ?
                                doc.getLong("likes").intValue() : 0;
                        String docId = doc.getId(); // Get the Firestore document ID
                        if (url != null) {
                            paintings.add(new Painting(url, docId, name, description, creationTime, likes));
                        }
                    }
                    numOfPaintings.setText("paintings - " + paintings.size());

                    if (paintings.isEmpty()) {
                        emptyPlaceholder.setVisibility(View.VISIBLE);
                        paintingsRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyPlaceholder.setVisibility(View.GONE);
                        paintingsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load paintings", Toast.LENGTH_SHORT).show();
                });
    }

    public void loadColor(Activity activity, View view) {
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("color", getResources().getColor(R.color.Default));
        view.setBackgroundColor(color);
    }
}
