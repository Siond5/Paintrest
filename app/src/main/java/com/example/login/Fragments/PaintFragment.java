package com.example.login.Fragments;

import static androidx.browser.customtabs.CustomTabsClient.getPackageName;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.login.Activities.MainActivity;
import com.example.login.Dialogs.PaintSettingsDialogFragment;
import com.example.login.R;
import com.example.login.Views.DrawingViewModel;
import com.example.login.Views.PaintView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class PaintFragment extends Fragment {

    private PaintView paintView;
    private static Bitmap savedBitmap;
    private DrawingViewModel drawingViewModel;
    private Button btn_undo;
    private Button btn_redo;

    public PaintFragment() {
        // Required empty constructor.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paint, container, false);
        loadColor(getActivity(), view);
        FrameLayout canvasContainer = view.findViewById(R.id.canvas_container);
        drawingViewModel = new ViewModelProvider(requireActivity()).get(DrawingViewModel.class);
        paintView = new PaintView(getActivity());
        canvasContainer.addView(paintView);

        Button btnClear = view.findViewById(R.id.btn_clear);
        Button btnPost = view.findViewById(R.id.btn_post);
        Button paintSettings = view.findViewById(R.id.paintSettings);
        btn_undo = view.findViewById(R.id.btn_undo);
        btn_redo = view.findViewById(R.id.btn_redo);

        btnClear.setOnClickListener(v -> {
            paintView.clearCanvasAndRecord();
            updateUndoRedoButtons();
            updateViewModelStacks();
        });
        btnPost.setOnClickListener(v -> showPostOptions());
        paintSettings.setOnClickListener(v -> openSettings());

        btn_undo.setOnClickListener(v -> {
            paintView.undo();
            updateUndoRedoButtons();
            updateViewModelStacks();
            // Reapply global settings from the ViewModel
            if (drawingViewModel.getCurrentTool().getValue() != null) {
                paintView.reapplyGlobalSettings(
                        drawingViewModel.getBrushColor().getValue(),
                        drawingViewModel.getBrushSize().getValue(),
                        drawingViewModel.getCurrentTool().getValue()
                );
            }
        });

        btn_redo.setOnClickListener(v -> {
            paintView.redo();
            updateUndoRedoButtons();
            updateViewModelStacks();
            // Reapply global settings from the ViewModel
            if (drawingViewModel.getCurrentTool().getValue() != null) {
                paintView.reapplyGlobalSettings(
                        drawingViewModel.getBrushColor().getValue(),
                        drawingViewModel.getBrushSize().getValue(),
                        drawingViewModel.getCurrentTool().getValue()
                );
            }
        });


        paintView.setOnPathRecordedCallback(() -> {
            updateUndoRedoButtons();
            updateViewModelStacks();
        });

        // Restore tool settings from the ViewModel.
        if (drawingViewModel.getBrushSize().getValue() != null)
            paintView.setBrushSize(drawingViewModel.getBrushSize().getValue());
        if (drawingViewModel.getCurrentTool().getValue() != null)
            paintView.setCurrentTool(drawingViewModel.getCurrentTool().getValue());
        if (drawingViewModel.getBrushColor().getValue() != null)
            paintView.setBrushColor(drawingViewModel.getBrushColor().getValue());
        if (drawingViewModel.getCurrentBitmap() != null)
            paintView.setBitmap(drawingViewModel.getCurrentBitmap());
        if (drawingViewModel.getUndoStack().getValue() != null && !drawingViewModel.getUndoStack().getValue().isEmpty())
            paintView.setUndoStack(drawingViewModel.getUndoStack().getValue());
        if (drawingViewModel.getRedoStack().getValue() != null && !drawingViewModel.getRedoStack().getValue().isEmpty())
            paintView.setRedoStack(drawingViewModel.getRedoStack().getValue());

        updateUndoRedoButtons();
        restoreCanvas();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (drawingViewModel.getBrushColor().getValue() != null)
            paintView.setBrushColor(drawingViewModel.getBrushColor().getValue());
        if (drawingViewModel.getBrushSize().getValue() != null)
            paintView.setBrushSize(drawingViewModel.getBrushSize().getValue());
        if (drawingViewModel.getCurrentTool().getValue() != null)
            paintView.setCurrentTool(drawingViewModel.getCurrentTool().getValue());
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCanvas();
        drawingViewModel.setCurrentBitmap(paintView.getBitmap());
        drawingViewModel.setBrushColor(paintView.getBrushColor());
        drawingViewModel.setBrushSize((float) paintView.getBrushSize());
        // Optionally update the tool as well:
        drawingViewModel.setCurrentTool(paintView.getCurrentTool());
        updateViewModelStacks();
    }

    private void updateViewModelStacks() {
        drawingViewModel.setUndoStack(new ArrayList<>(paintView.getUndoStack()));
        drawingViewModel.setRedoStack(new ArrayList<>(paintView.getRedoStack()));
    }

    private void updateUndoRedoButtons() {
        btn_undo.setEnabled(paintView.canUndo());
        btn_redo.setEnabled(paintView.canRedo());
    }

    private void saveCanvas() {
        savedBitmap = Bitmap.createBitmap(paintView.getWidth(), paintView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(savedBitmap);
        paintView.draw(canvas);
    }

    private void restoreCanvas() {
        if (savedBitmap != null)
            paintView.setBitmap(savedBitmap);
    }

    private void showPostOptions() {
        String[] options = {"Publish the paint", "Share the paint"};
        new AlertDialog.Builder(getContext())
                .setTitle("Post painting")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        publishCanvas();
                    } else if (which == 1) {
                        shareImage();
                    }
                })
                .show();
    }

    private void shareImage() {
        Bitmap painting = paintView.getBitmap();
        try {

            File cachePath = new File(getContext().getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }

            File file = new File(cachePath, "image.png");
            FileOutputStream stream = new FileOutputStream(file);
            painting.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(getContext(), "com.example.login.fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Image"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
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
                    drawingViewModel.setBrushColor(color);
                    drawingViewModel.setBrushSize((float) size);
                    drawingViewModel.setCurrentTool(tool);
                }
        );
    }

    public void loadColor(Activity activity, View view) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("color", R.color.Default);
        view.setBackgroundColor(color);
    }
}
