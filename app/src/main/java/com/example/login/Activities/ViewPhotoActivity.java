/**
 * Activity for displaying a full-screen photo and handling return navigation.
 *
 * Retrieves a photo URI from intent extras and displays it in an ImageView.
 * Provides a back button to finish the activity.
 */
package com.example.login.Activities;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.login.R;

public class ViewPhotoActivity extends Activity implements View.OnClickListener {
    /**
     * Button to navigate back to the previous screen.
     */
    private Button btn_back;

    /**
     * Called when the activity is created.
     * Initializes UI elements and loads the photo from intent extras.
     *
     * @param savedInstanceState Saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo);

        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        ImageView ivPhoto = findViewById(R.id.ivPhoto);
        String photoUri = getIntent().getStringExtra("photoUri");

        if (photoUri != null) {
            ivPhoto.setImageURI(Uri.parse(photoUri));
        } else {
            Toast.makeText(this, "No photo available.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles click events for the back button to finish the activity.
     *
     * @param view The clicked view
     */
    @Override
    public void onClick(View view) {
        if (view == btn_back) {
            finish();
        }
    }
}