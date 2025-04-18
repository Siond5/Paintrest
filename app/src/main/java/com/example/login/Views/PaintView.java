package com.example.login.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.example.login.Classes.PaintPath;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class PaintView extends View {

    private Paint paint;
    private Path path;
    private int brushColor = Color.BLACK;
    private float brushSize = 10;
    // Global current tool for new actions.
    private String currentTool = "Brush";
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private float startX, startY, endX, endY;
    private boolean isDrawingLine = false;

    // Undo and redo stacks.
    private Deque<PaintPath> paintPaths = new ArrayDeque<>();
    private Deque<PaintPath> redoPaths = new ArrayDeque<>();

    // Callback for state changes.
    private Runnable onPathRecordedCallback;

    public PaintView(Context context) {
        super(context);
        init();
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        updateBrush();
        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            // Fill with white background initially.
            bitmap.eraseColor(Color.WHITE);
        } else {
            Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas newCanvas = new Canvas(newBitmap);
            newCanvas.drawBitmap(bitmap, 0, 0, null);
            bitmap = newBitmap;
            bitmapCanvas = newCanvas;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, null);
        if ("Line".equals(currentTool) && isDrawingLine) {
            canvas.drawLine(startX, startY, endX, endY, paint);
        } else if (!path.isEmpty()) {
            canvas.drawPath(path, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();

        if ("Fill".equals(currentTool)) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                floodFill((int) x, (int) y, brushColor);
                Bitmap fillSnapshot = bitmap.copy(bitmap.getConfig(), true);
                recordFill(fillSnapshot);
                invalidate();
                return true;
            }
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if ("Line".equals(currentTool)) {
                    startX = x;
                    startY = y;
                    isDrawingLine = true;
                } else {
                    path = new Path();
                    path.moveTo(x, y);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if ("Line".equals(currentTool)) {
                    endX = x;
                    endY = y;
                } else {
                    path.lineTo(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                if ("Line".equals(currentTool)) {
                    bitmapCanvas.drawLine(startX, startY, endX, endY, paint);
                    recordAction(createLinePath(startX, startY, endX, endY));
                    isDrawingLine = false;
                } else {
                    bitmapCanvas.drawPath(path, paint);
                    recordAction(path);
                    path = new Path();
                }
                break;
        }
        invalidate();
        return true;
    }

    private Path createLinePath(float startX, float startY, float endX, float endY) {
        Path linePath = new Path();
        linePath.moveTo(startX, startY);
        linePath.lineTo(endX, endY);
        return linePath;
    }

    // Records a freehand or line action.
    private PaintPath recordAction(Path actionPath) {
        PaintPath toolAction = new PaintPath(actionPath, brushColor, brushSize, currentTool);
        paintPaths.push(toolAction);
        redoPaths.clear();
        if (onPathRecordedCallback != null) {
            onPathRecordedCallback.run();
        }
        return toolAction;
    }

    // Records a fill action with a snapshot.
    private PaintPath recordFill(Bitmap fillSnapshot) {
        PaintPath fillAction = new PaintPath(new Path(), brushColor, brushSize, "fill");
        fillAction.bitmapSnapshot = fillSnapshot;
        paintPaths.push(fillAction);
        redoPaths.clear();
        if (onPathRecordedCallback != null) {
            onPathRecordedCallback.run();
        }
        return fillAction;
    }

    // Clears the canvas (sets white background) and records a clear action.
    public void clearCanvasAndRecord() {
        if (bitmapCanvas != null) {
            // Clear to white.
            bitmapCanvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
        }
        // Push a clear marker; note that we do not wipe out previous actions so they remain in our undo history.
        PaintPath clearMarker = new PaintPath(new Path(), brushColor, brushSize, "clear");
        paintPaths.push(clearMarker);
        redoPaths.clear();
        if (onPathRecordedCallback != null) {
            onPathRecordedCallback.run();
        }
        rebuildBitmap();
        invalidate();
    }

    // Undo: remove the last action.
    public void undo() {
        if (!paintPaths.isEmpty()) {
            PaintPath action = paintPaths.pop();
            redoPaths.push(action);
            rebuildBitmap();
            invalidate();
        }
    }

    // Redo: reapply the last undone action.
    public void redo() {
        if (!redoPaths.isEmpty()) {
            PaintPath action = redoPaths.pop();
            paintPaths.push(action);
            rebuildBitmap();
            invalidate();
        }
    }

    // Setter to restore the redo stack.
    public void setRedoStack(List<PaintPath> stack) {
        redoPaths.clear();
        if (stack != null) {
            redoPaths.addAll(stack);
        }
    }

    // Rebuilds the bitmap by replaying actions.
    private void rebuildBitmap() {
        if (bitmapCanvas != null) {
            // Start with a white canvas.
            bitmapCanvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
        }
        // If the most recent action is a clear marker, we leave the canvas white.
        if (!paintPaths.isEmpty() && "clear".equals(paintPaths.peek().tool)) {
            return;
        }
        List<PaintPath> actions = new ArrayList<>(paintPaths);
        Collections.reverse(actions);
        for (PaintPath action : actions) {
            switch (action.tool) {
                case "clear":
                    bitmapCanvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
                    break;
                case "fill":
                    if (action.bitmapSnapshot != null) {
                        bitmapCanvas.drawBitmap(action.bitmapSnapshot, 0, 0, null);
                    } else {
                        bitmapCanvas.drawColor(action.color, PorterDuff.Mode.SRC);
                    }
                    break;
                case "Eraser":
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    paint.setStrokeWidth(action.strokeWidth);
                    bitmapCanvas.drawPath(action.path, paint);
                    paint.setXfermode(null);
                    paint.setColor(brushColor);
                    break;
                default:
                    paint.setXfermode(null);
                    paint.setColor(action.color);
                    paint.setStrokeWidth(action.strokeWidth);
                    bitmapCanvas.drawPath(action.path, paint);
                    break;
            }
        }
    }

    public void setOnPathRecordedCallback(Runnable callback) {
        this.onPathRecordedCallback = callback;
    }

    public Deque<PaintPath> getUndoStack() {
        return paintPaths;
    }

    public Deque<PaintPath> getRedoStack() {
        return redoPaths;
    }

    public boolean canUndo() {
        return !paintPaths.isEmpty();
    }

    public boolean canRedo() {
        return !redoPaths.isEmpty();
    }

    public void setUndoStack(List<PaintPath> stack) {
        paintPaths.clear();
        if (stack != null) {
            paintPaths.addAll(stack);
        }
        rebuildBitmap();
        invalidate();
    }

    public void setBrushColor(int color) {
        this.brushColor = color;
        updateBrush();
    }

    public void setBrushSize(float size) {
        this.brushSize = size;
        updateBrush();
    }

    // When setting the tool, update currentTool and configure paint accordingly.
    public void setCurrentTool(String tool) {
        this.currentTool = tool;
        if ("Eraser".equals(tool)) {
            currentPaintSetupForEraser();
        } else {
            paint.setXfermode(null);
            paint.setColor(brushColor);
        }
    }

    public String getCurrentTool() {
        return currentTool;
    }

    public int getBrushColor() {
        return brushColor;
    }

    public int getBrushSize() {
        return (int) brushSize;
    }

    private void updateBrush() {
        paint.setColor(brushColor);
        paint.setStrokeWidth(brushSize);
        invalidate();
    }

    // Configures the paint for eraser strokes.
    private void currentPaintSetupForEraser() {
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setStrokeWidth(brushSize);
    }

    public void reapplyGlobalSettings(int color, float brushSize, String tool) {
        setBrushColor(color);
        setBrushSize(brushSize);
        setCurrentTool(tool);
    }


    // Flood fill implementation remains unchanged.
    private void floodFill(int x, int y, int newColor) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int oldColor = pixels[y * width + x];
        if (oldColor == newColor) return;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});
        java.util.BitSet visited = new java.util.BitSet(width * height);
        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int px = point[0], py = point[1];
            int index = py * width + px;
            if (px < 0 || py < 0 || px >= width || py >= height || visited.get(index) || pixels[index] != oldColor)
                continue;
            pixels[index] = newColor;
            visited.set(index);
            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        invalidate();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap savedBitmap) {
        if (savedBitmap != null) {
            bitmap = savedBitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmapCanvas = new Canvas(bitmap);
            invalidate();
        }
    }

    public void resetView() {
        // Fill the internal bitmap with white.
        if (bitmapCanvas != null) {
            bitmapCanvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
        }
        // Clear the undo/redo stacks.
        paintPaths.clear();
        redoPaths.clear();
        // Optionally, set currentBitmap to a white image.
        // (Alternatively, clearBitmap may already have been done via drawColor above.)
        invalidate();
    }

}
