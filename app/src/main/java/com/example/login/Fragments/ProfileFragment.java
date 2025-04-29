package com.example.login.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.login.Activities.ViewPaintingActivity;
import com.example.login.Classes.Painting;
import com.example.login.Dialogs.LoadingManagerDialog;
import com.example.login.Views.PaintingItemView;
import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying the current user's profile information and their paintings.
 * <p>
 * Shows the user's name, profile image, and a grid of their paintings.
 * Supports viewing individual painting details and handles empty state.
 * </p>
 */
public class ProfileFragment extends Fragment {

    /** Request code for launching the painting detail activity. */
    private static final int REQUEST_CODE_VIEW_PAINTING = 1001;

    /** SharedPreferences for retrieving user details and settings. */
    private SharedPreferences sharedPreferences;

    /** TextView showing the number of paintings. */
    private TextView numOfPaintings, FullName;

    /** ImageView displaying the user's profile image. */
    private ImageView profileImage;

    /** RecyclerView displaying the user's paintings in a grid. */
    private RecyclerView paintingsRecyclerView;

    /** Placeholder view shown when there are no paintings. */
    private TextView emptyPlaceholder;

    /** Scrollable container for profile content. */
    private View scrollView;

    /** List backing the RecyclerView adapter. */
    private List<Painting> paintings = new ArrayList<>();

    /** Adapter for painting items. */
    private PaintingItemView adapter;

    /**
     * Required empty public constructor.
     */
    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Called to do initial creation of the fragment.
     *
     * @param savedInstanceState If the fragment is being re-created, this contains the previous state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     * <p>
     * Inflates the profile layout, initializes UI components, and loads user details and paintings.
     * </p>
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views.
     * @param container          If non-null, this is the parent view the fragment's UI should attach to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root View of the fragment's layout.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        loadBgColor(getActivity(), view);

        numOfPaintings = view.findViewById(R.id.numOfPaintings);
        paintingsRecyclerView = view.findViewById(R.id.paintingsRecyclerView);
        emptyPlaceholder = view.findViewById(R.id.emptyPlaceholder);
        FullName = view.findViewById(R.id.FullName);
        profileImage = view.findViewById(R.id.profileImage);
        scrollView = view.findViewById(R.id.scrollView);
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

    /**
     * Called when the fragment is visible to the user and actively running.
     * Reloads the user's paintings.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadUserPaintings();
    }

    /**
     * Loads the user's name and profile image from SharedPreferences.
     * Shows a toast on failure.
     */
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

    /**
     * Receives results from ViewPaintingActivity and refreshes the list if needed.
     *
     * @param requestCode The integer request code originally supplied.
     * @param resultCode  The integer result code returned by the child activity.
     * @param data        An Intent, which can return result data to the caller.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VIEW_PAINTING && resultCode == Activity.RESULT_OK) {
            // Refresh the paintings after deletion or any modification.
            loadUserPaintings();
        }
    }

    /**
     * Loads the current user's paintings from Firestore, updates the RecyclerView,
     * and handles empty state visibility.
     */
    private void loadUserPaintings() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        LoadingManagerDialog.showLoading(getActivity(), "Loading paintings...");
        FirebaseFirestore.getInstance().collection("paintings")
                .whereEqualTo("uid", currentUid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    paintings.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String url = doc.getString("imageUrl");
                        String uid = doc.getString("uid");
                        String name = doc.getString("name");
                        String description = doc.getString("description");
                        Boolean isAnonymous = doc.getBoolean("isAnonymous");
                        boolean anonymous = (isAnonymous != null) ? isAnonymous : false;
                        long creationTime = doc.getTimestamp("date") != null ?
                                doc.getTimestamp("date").toDate().getTime() : 0;
                        int likes = doc.getLong("likes") != null ? doc.getLong("likes").intValue() : 0;
                        String docId = doc.getId(); // Get the Firestore document ID

                        // Get the likedBy list
                        List<String> likedBy = (List<String>) doc.get("likedBy");

                        if (url != null) {
                            Painting painting = new Painting(url, docId, uid, name, description, creationTime, likes, anonymous, "");
                            painting.setLikedBy(likedBy); // Set the likedBy list
                            paintings.add(painting);
                        }
                    }
                    numOfPaintings.setText("paintings - " + paintings.size());

                    if (paintings.isEmpty()) {
                        emptyPlaceholder.setVisibility(View.VISIBLE);
                        scrollView.setVisibility(View.GONE);
                    } else {
                        emptyPlaceholder.setVisibility(View.GONE);
                        paintingsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                    LoadingManagerDialog.hideLoading();
                })
                .addOnFailureListener(e -> {
                    LoadingManagerDialog.hideLoading();
                    Toast.makeText(getContext(), "Failed to load paintings", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads the saved background color from SharedPreferences and applies it to the view.
     *
     * @param activity Activity context for accessing SharedPreferences.
     * @param view     The root view to apply the background color to.
     */
    public void loadBgColor(Activity activity, View view) {
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("bgColor", getResources().getColor(R.color.Default));
        view.setBackgroundColor(color);
    }
}
