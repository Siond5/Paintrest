package com.example.login.Views;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.login.Classes.PaintPath;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for drawing operations, managing undo/redo stacks, brush settings,
 * recent colors, and canvas state events.
 */
public class DrawingViewModel extends ViewModel {
    /**
     * Stack of paths for undo operations.
     */
    private final MutableLiveData<List<PaintPath>> undoStack = new MutableLiveData<>(new ArrayList<>());

    /**
     * Stack of paths for redo operations.
     */
    private final MutableLiveData<List<PaintPath>> redoStack = new MutableLiveData<>(new ArrayList<>());

    /**
     * List of recently used brush colors.
     */
    private final MutableLiveData<List<Integer>> recentColors = new MutableLiveData<>(new ArrayList<>());

    /**
     * Current bitmap representing the canvas content.
     */
    private Bitmap currentBitmap;

    /**
     * Flag indicating whether the user has logged out.
     */
    private static boolean hasLogOut = false;

    /**
     * Current brush color.
     */
    private final MutableLiveData<Integer> brushColor = new MutableLiveData<>(Color.BLACK);

    /**
     * Current brush size.
     */
    private final MutableLiveData<Float> brushSize = new MutableLiveData<>(10f);

    /**
     * Currently selected drawing tool (e.g., Brush, Eraser).
     */
    private final MutableLiveData<String> currentTool = new MutableLiveData<>("Brush");

    /**
     * Event flag for clearing the canvas.
     */
    private final MutableLiveData<Boolean> clearCanvasEvent = new MutableLiveData<>(false);

    /**
     * Returns live data for the undo stack.
     *
     * @return LiveData list of PaintPath for undo.
     */
    public LiveData<List<PaintPath>> getUndoStack() {
        return undoStack;
    }

    /**
     * Sets a new undo stack.
     *
     * @param stack List of PaintPath to use as undo stack.
     */
    public void setUndoStack(List<PaintPath> stack) {
        undoStack.setValue(stack);
    }

    /**
     * Returns live data for the redo stack.
     *
     * @return LiveData list of PaintPath for redo.
     */
    public LiveData<List<PaintPath>> getRedoStack() {
        return redoStack;
    }

    /**
     * Sets a new redo stack.
     *
     * @param stack List of PaintPath to use as redo stack.
     */
    public void setRedoStack(List<PaintPath> stack) {
        redoStack.setValue(stack);
    }

    /**
     * Returns live data of recent colors.
     *
     * @return LiveData list of Integer colors.
     */
    public LiveData<List<Integer>> getRecentColors() {
        return recentColors;
    }

    /**
     * Updates the list of recent colors.
     *
     * @param colors List of Integer colors.
     */
    public void setRecentColors(List<Integer> colors) {
        recentColors.setValue(colors);
    }

    /**
     * Gets the current canvas bitmap.
     *
     * @return Current Bitmap of the canvas.
     */
    public Bitmap getCurrentBitmap() {
        return currentBitmap;
    }

    /**
     * Sets the current canvas bitmap.
     *
     * @param bitmap Bitmap to set as current canvas.
     */
    public void setCurrentBitmap(Bitmap bitmap) {
        this.currentBitmap = bitmap;
    }

    /**
     * Returns live data for the brush color.
     *
     * @return LiveData Integer representing brush color.
     */
    public LiveData<Integer> getBrushColor() {
        return brushColor;
    }

    /**
     * Updates the brush color.
     *
     * @param color Integer color to set.
     */
    public void setBrushColor(int color) {
        brushColor.setValue(color);
    }

    /**
     * Returns live data for the brush size.
     *
     * @return LiveData Float representing brush size.
     */
    public LiveData<Float> getBrushSize() {
        return brushSize;
    }

    /**
     * Updates the brush size.
     *
     * @param size Float value for brush size.
     */
    public void setBrushSize(float size) {
        brushSize.setValue(size);
    }

    /**
     * Returns live data for the current drawing tool.
     *
     * @return LiveData String of tool name.
     */
    public LiveData<String> getCurrentTool() {
        return currentTool;
    }

    /**
     * Sets the current drawing tool.
     *
     * @param tool String name of the tool.
     */
    public void setCurrentTool(String tool) {
        currentTool.setValue(tool);
    }

    /**
     * Checks if the user has logged out.
     *
     * @return boolean logout status.
     */
    public boolean getHasLogOut() {
        return hasLogOut;
    }

    /**
     * Sets the logout flag.
     *
     * @param hasLogOut boolean indicating logout state.
     */
    public void setHasLogOut(boolean hasLogOut) {
        DrawingViewModel.hasLogOut = hasLogOut;
    }

    /**
     * Returns live data for the clear canvas event.
     *
     * @return LiveData Boolean of clear event.
     */
    public LiveData<Boolean> getClearCanvasEvent() {
        return clearCanvasEvent;
    }

    /**
     * Triggers a clear canvas event.
     */
    public void triggerClearCanvas() {
        clearCanvasEvent.setValue(true);
    }

    /**
     * Resets the clear canvas event flag.
     */
    public void resetClearCanvasEvent() {
        clearCanvasEvent.setValue(false);
    }

    /**
     * Resets the entire drawing model state to defaults.
     * <p>
     * Clears undo/redo stacks, resets bitmap, brush color, size, and tool.
     * </p>
     */
    public void reset() {
        setUndoStack(new ArrayList<>());
        setRedoStack(new ArrayList<>());
        currentBitmap = null;
        brushColor.setValue(Color.BLACK);
        brushSize.setValue(10f);
        currentTool.setValue("Brush");
    }
}
