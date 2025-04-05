package com.example.login.Views;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.login.Classes.PaintPath;
import java.util.ArrayList;
import java.util.List;

public class DrawingViewModel extends ViewModel {
    private final MutableLiveData<List<PaintPath>> undoStack = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PaintPath>> redoStack = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Integer>> recentColors = new MutableLiveData<>(new ArrayList<>());
    private Bitmap currentBitmap;
    private final MutableLiveData<Integer> brushColor = new MutableLiveData<>(Color.BLACK);
    private final MutableLiveData<Float> brushSize = new MutableLiveData<>(10f);
    private final MutableLiveData<String> currentTool = new MutableLiveData<>("Brush");

    public LiveData<List<PaintPath>> getUndoStack() { return undoStack; }
    public void setUndoStack(List<PaintPath> stack) { undoStack.setValue(stack); }
    public LiveData<List<PaintPath>> getRedoStack() { return redoStack; }
    public void setRedoStack(List<PaintPath> stack) { redoStack.setValue(stack); }
    public LiveData<List<Integer>> getRecentColors() { return recentColors; }
    public void setRecentColors(List<Integer> colors) { recentColors.setValue(colors); }
    public Bitmap getCurrentBitmap() { return currentBitmap; }
    public void setCurrentBitmap(Bitmap bitmap) { this.currentBitmap = bitmap; }
    public LiveData<Integer> getBrushColor() { return brushColor; }
    public void setBrushColor(int color) { brushColor.setValue(color); }
    public LiveData<Float> getBrushSize() { return brushSize; }
    public void setBrushSize(float size) { brushSize.setValue(size); }
    public LiveData<String> getCurrentTool() { return currentTool; }
    public void setCurrentTool(String tool) { currentTool.setValue(tool); }
}
