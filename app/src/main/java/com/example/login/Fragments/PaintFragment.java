package com.example.login.Fragments;

import static android.text.TextUtils.TruncateAt.END;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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

import com.example.login.Dialogs.PaintSettingsDialogFragment;
import com.example.login.Dialogs.PublishDialogFragment;
import com.example.login.R;
import com.example.login.Views.DrawingViewModel;
import com.example.login.Views.PaintView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Fragment that provides a drawing canvas and controls for painting.
 * <p>
 * Hosts a PaintView, manages brush/eraser tools, undo/redo stacks,
 * and supports publishing or sharing the resulting image.
 * State is persisted via a DrawingViewModel.
 * </p>
 */
public class PaintFragment extends Fragment {

    /** The custom view where users draw. */
    private PaintView paintView;

    /** Cached bitmap of the last saved canvas. */
    private static Bitmap savedBitmap;

    /** ViewModel storing brush settings and undo/redo stacks. */
    private DrawingViewModel drawingViewModel;

    /** Button for undoing the last action. */
    private Button btn_undo;

    /** Button for redoing the last undone action. */
    private Button btn_redo;

    /** SharedPreferences for persisting UI colors and user settings. */
    private SharedPreferences sharedPreferences;

    /**
     * Required empty public constructor.
     */
    public PaintFragment() {
        // Required empty constructor.
    }

    /**
     * Inflates the fragment's UI, sets up the PaintView and control buttons,
     * and restores any saved state from the ViewModel.
     *
     * @param inflater           LayoutInflater to inflate the layout.
     * @param container          Parent view to attach the fragment's UI.
     * @param savedInstanceState Bundle containing saved state, if any.
     * @return The root View of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paint, container, false);
        loadBgColor(getActivity(), view);
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

        // Show the two-option dialog when the user presses the post button.
        btnPost.setOnClickListener(v -> showPostOptions());

        paintSettings.setOnClickListener(v -> openSettings());

        btn_undo.setOnClickListener(v -> {
            paintView.undo();
            updateUndoRedoButtons();
            updateViewModelStacks();
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

        // Restore tool settings and canvas state from the ViewModel.
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

        // Observe the clearCanvas event from the ViewModel.
        drawingViewModel.getClearCanvasEvent().observe(getViewLifecycleOwner(), shouldClear -> {
            if (shouldClear != null && shouldClear) {
                paintView.clearCanvasAndRecord();
                updateUndoRedoButtons();
                updateViewModelStacks();
                drawingViewModel.resetClearCanvasEvent();
            }
        });

        return view;
    }

    /**
     * Ensures brush and tool settings persist when fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (drawingViewModel.getHasLogOut()) {
            resetPainting();
            drawingViewModel.setHasLogOut(false);
            return;
        }
        if (drawingViewModel.getBrushColor().getValue() != null)
            paintView.setBrushColor(drawingViewModel.getBrushColor().getValue());
        if (drawingViewModel.getBrushSize().getValue() != null)
            paintView.setBrushSize(drawingViewModel.getBrushSize().getValue());
        if (drawingViewModel.getCurrentTool().getValue() != null)
            paintView.setCurrentTool(drawingViewModel.getCurrentTool().getValue());
    }

    /**
     * Saves canvas and tool state when fragment is paused.
     */
    @Override
    public void onPause() {
        super.onPause();
        saveCanvas();
        drawingViewModel.setCurrentBitmap(paintView.getBitmap());
        drawingViewModel.setBrushColor(paintView.getBrushColor());
        drawingViewModel.setBrushSize((float) paintView.getBrushSize());
        drawingViewModel.setCurrentTool(paintView.getCurrentTool());
        updateViewModelStacks();
    }

    /**
     * Updates the ViewModel's undo/redo stacks.
     */
    private void updateViewModelStacks() {
        drawingViewModel.setUndoStack(new ArrayList<>(paintView.getUndoStack()));
        drawingViewModel.setRedoStack(new ArrayList<>(paintView.getRedoStack()));
    }

    /**
     * Resets the painting to initial state (clear, default tool and color).
     */
    private void resetPainting() {
        paintView.clearCanvasAndRecord();

        PaintSettingsDialogFragment.clearRecentColors();

        paintView.setBrushColor(Color.BLACK);
        paintView.setBrushSize(10);
        paintView.setCurrentTool("Brush");

        drawingViewModel.setBrushColor(Color.BLACK);
        drawingViewModel.setBrushSize(10f);
        drawingViewModel.setCurrentTool("Brush");

        paintView.setUndoStack(null);
        updateUndoRedoButtons();
    }

    /**
     * Enables or disables the undo/redo buttons based on availability.
     */
    private void updateUndoRedoButtons() {
        btn_undo.setEnabled(paintView.canUndo());
        btn_redo.setEnabled(paintView.canRedo());
    }

    /**
     * Saves the current canvas into a static bitmap for restoration.
     */
    private void saveCanvas() {
        savedBitmap = Bitmap.createBitmap(paintView.getWidth(), paintView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(savedBitmap);
        paintView.draw(canvas);
    }

    /**
     * Restores the saved bitmap onto the PaintView.
     */
    private void restoreCanvas() {
        if (savedBitmap != null)
            paintView.setBitmap(savedBitmap);
    }

    /**
     * Shows a dialog offering to publish or share the painting.
     */
    private void showPostOptions() {
        String[] options = {"Publish the paint", "Share the paint"};
        new AlertDialog.Builder(getContext())
                .setTitle("Post Painting")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openPublishDialog();
                    } else if (which == 1) {
                        sharePainting();
                    }
                })
                .show();
    }

    /**
     * Opens the publish dialog with a white-backed bitmap of the canvas.
     */
    private void openPublishDialog() {
        paintView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(paintView.getDrawingCache());
        paintView.setDrawingCacheEnabled(false);

        Bitmap finalBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas finalCanvas = new Canvas(finalBitmap);
        finalCanvas.drawColor(Color.WHITE);
        finalCanvas.drawBitmap(bitmap, 0, 0, null);

        PublishDialogFragment publishDialog = PublishDialogFragment.newInstance(finalBitmap);
        publishDialog.show(getChildFragmentManager(), "publishDialog");
    }

    /**
     * Shares the painting via external apps using a FileProvider.
     */
    private void sharePainting() {
        paintView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(paintView.getDrawingCache());
        paintView.setDrawingCacheEnabled(false);
        Bitmap finalBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas finalCanvas = new Canvas(finalBitmap);
        finalCanvas.drawColor(Color.WHITE);
        finalCanvas.drawBitmap(bitmap, 0, 0, null);

        try {
            File cachePath = new File(getContext().getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            File file = new File(cachePath, "image.png");
            FileOutputStream stream = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(getContext(), "com.example.login.provider", file);

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

    /**
     * Opens the paint settings dialog to adjust brush color, size, and tool.
     */
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

    /**
     * Returns the PaintView instance contained in this fragment.
     *
     * @return the PaintView for direct access.
     */
    public PaintView getPaintView() {
        return paintView;
    }

    /**
     * Loads background color and button tint from SharedPreferences.
     *
     * @param activity Activity context for SharedPreferences.
     * @param view     Root view to apply background color.
     */
    public void loadBgColor(Activity activity, View view) {
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("bgColor", R.color.Default);
        view.setBackgroundColor(color);
        loadBtnColor((ViewGroup) view);
    }

    /**
     * Recursively applies the stored button color to all Button views.
     *
     * @param rootView ViewGroup whose child buttons will be tinted.
     */
    private void loadBtnColor(ViewGroup rootView) {
        int color = sharedPreferences.getInt("btnColor", R.color.button);
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
