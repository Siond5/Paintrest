package com.example.login.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.view.View;

import com.example.login.R;

public class ColorPickerView extends LinearLayout {
    private float selectedHue = 0f;
    private float selectedSaturation = 1f;
    private float selectedBrightness = 1f;
    private SeekBar hueSeekBar;
    private ImageView saturationBrightnessView;
    private View previewBox;

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_color_picker, this, true);
        hueSeekBar = findViewById(R.id.hueSeekBar);
        saturationBrightnessView = findViewById(R.id.saturationBrightnessView);
        previewBox = findViewById(R.id.previewBox);

        hueSeekBar.setMax(360);
        hueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedHue = progress;
                updateViews();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        saturationBrightnessView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = Math.max(0, Math.min(event.getX(), v.getWidth()));
                float y = Math.max(0, Math.min(event.getY(), v.getHeight()));
                selectedSaturation = x / v.getWidth();
                selectedBrightness = 1 - (y / v.getHeight());
                updateViews();
            }
            return true;
        });
    }

    private void updateViews() {
        previewBox.setBackgroundColor(Color.HSVToColor(new float[]{selectedHue, selectedSaturation, selectedBrightness}));

        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                float sat = x / 255f;
                float bright = 1 - (y / 255f);
                bitmap.setPixel(x, y, Color.HSVToColor(new float[]{selectedHue, sat, bright}));
            }
        }
        saturationBrightnessView.setImageBitmap(bitmap);
    }

    public int getSelectedColor() {
        return Color.HSVToColor(new float[]{selectedHue, selectedSaturation, selectedBrightness});
    }
}
