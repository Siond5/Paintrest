package com.example.login.Activities;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.login.R;


public class ViewPhotoActivity extends Activity implements View.OnClickListener{

    private Button btn_back;

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

    @Override
    public void onClick(View view) {
        if (view == btn_back)
        {
            finish();
        }
    }
}

