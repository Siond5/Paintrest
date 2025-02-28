package com.example.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Date;

public class PaintFragment extends Fragment {

    private PaintView paintView;
    private SharedPreferences sp;

    public PaintFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paint, container, false);

        FrameLayout canvasContainer = view.findViewById(R.id.canvas_container);
        paintView = new PaintView(getActivity());
        canvasContainer.addView(paintView);

        Button btnClear = view.findViewById(R.id.btn_clear);
        Button btnPublish = view.findViewById(R.id.btn_publish);

        btnClear.setOnClickListener(v -> paintView.clearCanvas());
        btnPublish.setOnClickListener(v -> publishCanvas());

        loadColor(getActivity(), view);
        return view;
    }

    private void publishCanvas() {
        paintView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(paintView.getDrawingCache());
        paintView.setDrawingCacheEnabled(false);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(getActivity(), "You are not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String filename = "paint_" + System.currentTimeMillis() + ".png";
        StorageReference canvasRef = storageRef.child("paintings").child(uid).child(filename);

        canvasRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(getActivity(), "The painting has been successfully Published!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to Publish the painting", Toast.LENGTH_SHORT).show();
                });
    }

    public void loadColor(Activity activity, View view){
        sp = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sp.getInt("color", R.color.Default);
        view.setBackgroundColor(color);    }
}
