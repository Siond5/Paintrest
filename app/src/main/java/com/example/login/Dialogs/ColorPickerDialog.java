package com.example.login.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.login.R;

public class ColorPickerDialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public static void show(Context context, int initialColor, OnColorSelectedListener listener) {
        Dialog dialog = new Dialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        dialog.setContentView(dialogView);

        ImageView saturationBrightnessView = dialogView.findViewById(R.id.saturationBrightnessView);
        SeekBar hueSeekBar = dialogView.findViewById(R.id.hueSeekBar);
        View previewBox = dialogView.findViewById(R.id.previewBox);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        final float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);

        hueSeekBar.setMax(360);
        hueSeekBar.setProgress((int) hsv[0]);

        setHueGradient(hueSeekBar);
        updateSaturationBrightnessView(saturationBrightnessView, hsv, true);
        updatePreviewBox(previewBox, hsv);

        hueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hsv[0] = progress;
                updateSaturationBrightnessView(saturationBrightnessView, hsv, true);
                updatePreviewBox(previewBox, hsv);
                setCustomThumb(hueSeekBar, hsv[0]);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        saturationBrightnessView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = Math.max(0, Math.min(event.getX(), v.getWidth()));
                float y = Math.max(0, Math.min(event.getY(), v.getHeight()));

                hsv[1] = x / v.getWidth();
                hsv[2] = 1 - (y / v.getHeight());

                updateSaturationBrightnessView(saturationBrightnessView, hsv, true);
                updatePreviewBox(previewBox, hsv);
            }
            return true;
        });

        btnOk.setOnClickListener(v -> {
            listener.onColorSelected(Color.HSVToColor(hsv));
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static void setHueGradient(SeekBar hueSeekBar) {
        int[] hueColors = new int[361];
        for (int i = 0; i <= 360; i++) {
            hueColors[i] = Color.HSVToColor(new float[]{i, 1, 1});
        }
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, hueColors);
        hueSeekBar.setProgressDrawable(gradientDrawable);
        setCustomThumb(hueSeekBar, hueSeekBar.getProgress());
    }

    private static void setCustomThumb(SeekBar hueSeekBar, float hue) {
        int thumbSize = 40;
        Bitmap thumbBitmap = Bitmap.createBitmap(thumbSize, thumbSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(thumbBitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.HSVToColor(new float[]{hue, 1f, 1f}));
        canvas.drawCircle(thumbSize / 2f, thumbSize / 2f, thumbSize / 3f, circlePaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        canvas.drawCircle(thumbSize / 2f, thumbSize / 2f, thumbSize / 3f, borderPaint);

        hueSeekBar.setThumb(new BitmapDrawable(hueSeekBar.getResources(), thumbBitmap));
    }

    private static void updateSaturationBrightnessView(ImageView imageView, float[] hsv, boolean drawThumb) {
        int width = 256, height = 256;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float sat = x / 255f;
                float bright = 1 - (y / 255f);
                bitmap.setPixel(x, y, Color.HSVToColor(new float[]{hsv[0], sat, bright}));
            }
        }

        if (drawThumb) {
            drawSelectionThumb(bitmap, hsv[1] * width, (1 - hsv[2]) * height);
        }

        imageView.setImageBitmap(bitmap);
    }

    private static void drawSelectionThumb(Bitmap bitmap, float x, float y) {
        Canvas canvas = new Canvas(bitmap);

        Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(Color.BLACK);
        thumbPaint.setStyle(Paint.Style.STROKE);
        thumbPaint.setStrokeWidth(3);

        canvas.drawCircle(x, y, 10, thumbPaint);
    }

    private static void updatePreviewBox(View previewBox, float[] hsv) {
        previewBox.setBackgroundColor(Color.HSVToColor(hsv));
    }
}
