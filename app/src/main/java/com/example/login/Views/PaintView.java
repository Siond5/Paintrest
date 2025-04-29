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

/**
 * Custom View for freehand painting, line drawing, erasing, filling, and undo/redo functionality.
 * <p>
 * Supports multiple tools (Brush, Line, Eraser, Fill), maintains action stacks,
 * and reconstructs the canvas state on undo/redo.
 * </p>
 */
public class PaintView extends View {

    /** Paint object used for drawing strokes and shapes. */
    private Paint paint;

    /** Path for the current freehand stroke. */
    private Path path;

    /** Current brush color (ARGB integer). */
    private int brushColor = Color.BLACK;

    /** Current brush stroke width in pixels. */
    private float brushSize = 10;

    /** Currently selected tool: "Brush", "Line", "Eraser", or "Fill". */
    private String currentTool = "Brush";

    /** Bitmap backing the canvas content. */
    private Bitmap bitmap;

    /** Canvas wrapping the bitmap for drawing operations. */
    private Canvas bitmapCanvas;

    /** Coordinates for line drawing start and end. */
    private float startX, startY, endX, endY;

    /** Flag indicating an in-progress line draw. */
    private boolean isDrawingLine = false;

    /** Stack of PaintPath actions for undo. */
    private Deque<PaintPath> paintPaths = new ArrayDeque<>();

    /** Stack of PaintPath actions for redo. */
    private Deque<PaintPath> redoPaths = new ArrayDeque<>();

    /** Callback invoked whenever a new path action is recorded. */
    private Runnable onPathRecordedCallback;

    /**
     * Constructor for programmatic instantiation.
     *
     * @param context The Context in which the view is running.
     */
    public PaintView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor called when inflating from XML.
     *
     * @param context The Context in which the view is running.
     * @param attrs   AttributeSet from XML.
     */
    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes paint settings and path object.
     */
    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        updateBrush();
        path = new Path();
    }

    /**
     * Handles size changes, creating or resizing the bitmap buffer.
     *
     * @param w    New width.
     * @param h    New height.
     * @param oldw Old width.
     * @param oldh Old height.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            bitmap.eraseColor(Color.WHITE);
        } else {
            Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas newCanvas = new Canvas(newBitmap);
            newCanvas.drawBitmap(bitmap, 0, 0, null);
            bitmap = newBitmap;
            bitmapCanvas = newCanvas;
        }
    }

    /**
     * Draws the background, bitmap buffer, and current in-progress stroke or line.
     *
     * @param canvas Canvas provided by the system.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        if ("Line".equals(currentTool) && isDrawingLine) {
            canvas.drawLine(startX, startY, endX, endY, paint);
        } else if (!path.isEmpty()) {
            canvas.drawPath(path, paint);
        }
    }

    /**
     * Handles touch events for drawing, line creation, filling, and erasing.
     *
     * @param event MotionEvent describing touch.
     * @return true if the event was handled.
     */
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

    /**
     * Creates a Path representing a straight line between two points.
     *
     * @param startX X-coordinate of start.
     * @param startY Y-coordinate of start.
     * @param endX   X-coordinate of end.
     * @param endY   Y-coordinate of end.
     * @return Path representing the line.
     */
    private Path createLinePath(float startX, float startY, float endX, float endY) {
        Path linePath = new Path();
        linePath.moveTo(startX, startY);
        linePath.lineTo(endX, endY);
        return linePath;
    }

    /**
     * Records a drawing action (freehand or line) into the undo stack.
     *
     * @param actionPath Path of the action.
     * @return PaintPath object recorded.
     */
    private PaintPath recordAction(Path actionPath) {
        PaintPath toolAction = new PaintPath(actionPath, brushColor, brushSize, currentTool);
        paintPaths.push(toolAction);
        redoPaths.clear();
        if (onPathRecordedCallback != null) onPathRecordedCallback.run();
        return toolAction;
    }

    /**
     * Records a fill action with a bitmap snapshot into the undo stack.
     *
     * @param fillSnapshot Bitmap after fill operation.
     * @return PaintPath object recorded.
     */
    private PaintPath recordFill(Bitmap fillSnapshot) {
        PaintPath fillAction = new PaintPath(new Path(), brushColor, brushSize, "fill");
        fillAction.bitmapSnapshot = fillSnapshot;
        paintPaths.push(fillAction);
        redoPaths.clear();
        if (onPathRecordedCallback != null) onPathRecordedCallback.run();
        return fillAction;
    }

    /**
     * Clears the canvas to transparent and records a clear action.
     */
    public void clearCanvasAndRecord() {
        if (bitmapCanvas != null) {
            bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        PaintPath clearMarker = new PaintPath(new Path(), brushColor, brushSize, "clear");
        paintPaths.push(clearMarker);
        redoPaths.clear();
        if (onPathRecordedCallback != null) onPathRecordedCallback.run();
        invalidate();
    }

    /**
     * Undoes the last action, moving it to redo stack and rebuilding the bitmap.
     */
    public void undo() {
        if (!paintPaths.isEmpty()) {
            PaintPath action = paintPaths.pop();
            redoPaths.push(action);
            rebuildBitmap();
            invalidate();
        }
    }

    /**
     * Redoes the last undone action, moving it back to undo stack and rebuilding.
     */
    public void redo() {
        if (!redoPaths.isEmpty()) {
            PaintPath action = redoPaths.pop();
            paintPaths.push(action);
            rebuildBitmap();
            invalidate();
        }
    }

    /**
     * Restores the redo stack from a list of PaintPath objects.
     *
     * @param stack List of PaintPath to restore.
     */
    public void setRedoStack(List<PaintPath> stack) {
        redoPaths.clear();
        if (stack != null) redoPaths.addAll(stack);
    }

    /**
     * Rebuilds the bitmap by replaying all actions in order.
     */
    private void rebuildBitmap() {
        if (bitmapCanvas != null) {
            bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        if (!paintPaths.isEmpty() && "clear".equals(paintPaths.peek().tool)) return;
        List<PaintPath> actions = new ArrayList<>(paintPaths);
        Collections.reverse(actions);
        for (PaintPath action : actions) {
            Paint tempPaint = new Paint();
            tempPaint.setAntiAlias(true);
            tempPaint.setStyle(Paint.Style.STROKE);
            tempPaint.setStrokeCap(Paint.Cap.ROUND);
            tempPaint.setStrokeJoin(Paint.Join.ROUND);
            switch (action.tool) {
                case "clear":
                    bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    break;
                case "fill":
                    if (action.bitmapSnapshot != null) {
                        bitmapCanvas.drawBitmap(action.bitmapSnapshot, 0, 0, null);
                    } else {
                        bitmapCanvas.drawColor(action.color);
                    }
                    break;
                case "Eraser":
                    tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    tempPaint.setStrokeWidth(action.strokeWidth);
                    bitmapCanvas.drawPath(action.path, tempPaint);
                    break;
                default:
                    tempPaint.setXfermode(null);
                    tempPaint.setColor(action.color);
                    tempPaint.setStrokeWidth(action.strokeWidth);
                    bitmapCanvas.drawPath(action.path, tempPaint);
                    break;
            }
        }
        updateBrush();
        if ("Eraser".equals(currentTool)) currentPaintSetupForEraser();
    }

    /**
     * Sets a callback to run whenever an action is recorded.
     *
     * @param callback Runnable to invoke.
     */
    public void setOnPathRecordedCallback(Runnable callback) {
        this.onPathRecordedCallback = callback;
    }

    /**
     * @return Deque of recorded paint paths (undo stack).
     */
    public Deque<PaintPath> getUndoStack() {
        return paintPaths;
    }

    /**
     * @return Deque of redo paths.
     */
    public Deque<PaintPath> getRedoStack() {
        return redoPaths;
    }

    /**
     * @return true if undo is possible.
     */
    public boolean canUndo() {
        return !paintPaths.isEmpty();
    }

    /**
     * @return true if redo is possible.
     */
    public boolean canRedo() {
        return !redoPaths.isEmpty();
    }

    /**
     * Restores the undo stack from a list and updates the canvas.
     *
     * @param stack List of PaintPath for undo.
     */
    public void setUndoStack(List<PaintPath> stack) {
        paintPaths.clear();
        if (stack != null) paintPaths.addAll(stack);
        rebuildBitmap();
        invalidate();
    }

    /**
     * Updates the brush color and reapplies paint settings.
     *
     * @param color ARGB integer color.
     */
    public void setBrushColor(int color) {
        this.brushColor = color;
        updateBrush();
    }

    /**
     * Updates the brush size and reapplies paint settings.
     *
     * @param size Stroke width in pixels.
     */
    public void setBrushSize(float size) {
        this.brushSize = size;
        updateBrush();
    }

    /**
     * Selects the drawing tool and configures paint for eraser if needed.
     *
     * @param tool Tool name ("Brush","Eraser","Line","Fill").
     */
    public void setCurrentTool(String tool) {
        this.currentTool = tool;
        if ("Eraser".equals(tool)) {
            currentPaintSetupForEraser();
        } else {
            paint.setXfermode(null);
            paint.setColor(brushColor);
        }
    }

    /**
     * @return Currently selected tool name.
     */
    public String getCurrentTool() {
        return currentTool;
    }

    /**
     * @return Current brush color (ARGB integer).
     */
    public int getBrushColor() {
        return brushColor;
    }

    /**
     * @return Current brush size as integer.
     */
    public int getBrushSize() {
        return (int) brushSize;
    }

    /**
     * Applies brush color and size to the Paint object and invalidates view.
     */
    private void updateBrush() {
        paint.setColor(brushColor);
        paint.setStrokeWidth(brushSize);
        invalidate();
    }

    /**
     * Configures Paint for eraser mode (clearing pixels).
     */
    private void currentPaintSetupForEraser() {
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setStrokeWidth(brushSize);
    }

    /**
     * Restores global brush and tool settings in one call.
     *
     * @param color     ARGB color to set.
     * @param brushSize Stroke width to set.
     * @param tool      Tool name to set.
     */
    public void reapplyGlobalSettings(int color, float brushSize, String tool) {
        setBrushColor(color);
        setBrushSize(brushSize);
        setCurrentTool(tool);
    }

    /**
     * Flood-fill algorithm starting at (x,y) replacing target color with newColor.
     *
     * @param x        X-coordinate to start fill.
     * @param y        Y-coordinate to start fill.
     * @param newColor ARGB integer of fill color.
     */
    private void floodFill(int x, int y, int newColor) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int targetIndex = y * width + x;
        if (targetIndex < 0 || targetIndex >= pixels.length) return;
        int oldColor = pixels[targetIndex];
        if (oldColor == 0) oldColor = Color.WHITE;
        if (oldColor == newColor) return;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});
        BitSet visited = new BitSet(width * height);
        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int px = point[0], py = point[1];
            int index = py * width + px;
            if (px < 0 || py < 0 || px >= width || py >= height || visited.get(index)) continue;
            int currentColor = pixels[index];
            if (currentColor == 0) currentColor = Color.WHITE;
            if (currentColor != oldColor) continue;
            pixels[index] = newColor;
            visited.set(index);
            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    /**
     * @return The internal bitmap representing the canvas.
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * Sets the internal bitmap from a saved state and invalidates the view.
     *
     * @param savedBitmap Bitmap to restore.
     */
    public void setBitmap(Bitmap savedBitmap) {
        if (savedBitmap != null) {
            bitmap = savedBitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmapCanvas = new Canvas(bitmap);
            invalidate();
        }
    }
}
