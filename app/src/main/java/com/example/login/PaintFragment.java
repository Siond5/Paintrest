package com.example.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class PaintFragment extends Fragment {

    private PaintView paintView;
    private static Bitmap savedBitmap;

    public PaintFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paint, container, false);
        loadColor(getActivity(), view);
        FrameLayout canvasContainer = view.findViewById(R.id.canvas_container);
        paintView = new PaintView(getActivity());
        canvasContainer.addView(paintView);

        Button btnClear = view.findViewById(R.id.btn_clear);
        Button btnPublish = view.findViewById(R.id.btn_publish);
        Button paintSettings = view.findViewById(R.id.paintSettings);

        btnClear.setOnClickListener(v -> {
            paintView.clearCanvas();
            savedBitmap = null;
        });

        btnPublish.setOnClickListener(v -> publishCanvas());
        paintSettings.setOnClickListener(v -> openSettings());

        restoreCanvas();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCanvas();
    }

    private void saveCanvas() {
        savedBitmap = Bitmap.createBitmap(paintView.getWidth(), paintView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(savedBitmap);
        paintView.draw(canvas);
    }

    private void restoreCanvas() {
        if (savedBitmap != null) {
            paintView.setBitmap(savedBitmap);
        }
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
            startActivity(new Intent(getActivity(), MainActivity.class));
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String filename = "paint_" + System.currentTimeMillis() + ".png";
        StorageReference canvasRef = storageRef.child("paintings").child(uid).child(filename);

        canvasRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot ->
                        Toast.makeText(getActivity(), "The painting has been successfully Published!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Failed to Publish the painting", Toast.LENGTH_SHORT).show()
                );
    }

    private void openSettings() {
        PaintSettingsDialogFragment.show(
                getContext(),
                paintView.getBrushColor(),
                (int) paintView.getBrushSize(),
                paintView.getCurrentTool(),
                (color, size, tool) -> {
                    paintView.setBrushColor(color);
                    paintView.setBrushSize(size);
                    paintView.setCurrentTool(tool);
                }
        );
    }

    public void loadColor(Activity activity, View view){
        SharedPreferences sharedPreferences;
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("color", R.color.Default);
        view.setBackgroundColor(color);
    }
}
