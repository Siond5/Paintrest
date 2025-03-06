package com.example.login.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;

public class PaintView extends View {

    private Paint paint;
    private Path path;
    private int brushColor = Color.BLACK;
    private float brushSize = 10;
    private String currentTool = "Brush";
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private float startX, startY, endX, endY;
    private boolean isDrawingLine = false;

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
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if ("Fill".equals(currentTool)) {
                    floodFill((int) x, (int) y, brushColor);
                } else if ("Line".equals(currentTool)) {
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
                } else if (!"Fill".equals(currentTool)) {
                    path.lineTo(x, y);
                }
                break;

            case MotionEvent.ACTION_UP:
                if ("Line".equals(currentTool)) {
                    bitmapCanvas.drawLine(startX, startY, endX, endY, paint);
                    isDrawingLine = false;
                } else if (!"Fill".equals(currentTool)) {
                    bitmapCanvas.drawPath(path, paint);
                    path.reset();
                }
                break;
        }
        invalidate();
        return true;
    }

    public void setBrushColor(int color) {
        this.brushColor = color;
        updateBrush();
    }

    public void setBrushSize(float size) {
        this.brushSize = size;
        updateBrush();
    }

    public void setCurrentTool(String tool) {
        this.currentTool = tool;
        if ("Eraser".equals(tool)) {
            paint.setColor(Color.WHITE);
        } else {
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

    private void floodFill(int x, int y, int newColor) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int oldColor = pixels[y * width + x];
        if (oldColor == newColor) return;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});
        BitSet visited = new BitSet(width * height);

        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int px = point[0], py = point[1];

            int index = py * width + px;
            if (px < 0 || py < 0 || px >= width || py >= height || visited.get(index) || pixels[index] != oldColor) {
                continue;
            }

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

    public void clearCanvas() {
        if (bitmapCanvas != null) {
            bitmapCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        }
        path.reset();
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
}
