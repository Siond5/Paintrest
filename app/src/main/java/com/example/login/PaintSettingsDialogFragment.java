package com.example.login;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

public class PaintSettingsDialogFragment {

    public interface OnPaintSettingsSelectedListener {
        void onSettingsSelected(int color, int brushSize, String tool);
    }

    private static final List<Integer> recentColors = new ArrayList<>();

    public static void show(Context context, int initialColor, int initialBrushSize, String initialTool, OnPaintSettingsSelectedListener listener) {
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_paint_settings_dialog, null);
        dialog.setContentView(view);

        SeekBar seekBarSize = view.findViewById(R.id.seekBarSize);
        Button btnColorPicker = view.findViewById(R.id.btnColorPicker);
        Button btnEraser = view.findViewById(R.id.btnEraser);
        Button btnLine = view.findViewById(R.id.btnLine);
        Button btnFill = view.findViewById(R.id.btnFill);
        Button btnBrush = view.findViewById(R.id.btnBrush);
        LinearLayout recentColorsLayout = view.findViewById(R.id.recentColorsLayout);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnApply = view.findViewById(R.id.btnApply);

        final int[] selectedColor = {initialColor};
        final String[] selectedTool = {initialTool};
        seekBarSize.setProgress(initialBrushSize);

        if (!recentColors.contains(Color.BLACK)) {
            recentColors.add(0, Color.BLACK);
        }

        updateButtonColor(btnColorPicker, selectedColor[0]);

        highlightSelectedTool(selectedTool[0], btnBrush, btnEraser, btnLine, btnFill);

        btnColorPicker.setOnClickListener(v -> ColorPickerDialog.show(context, selectedColor[0], color -> {
            animateColorTransition(btnColorPicker, selectedColor[0], color);
            selectedColor[0] = color;
            addRecentColor(color, recentColorsLayout, context, selectedColor, btnColorPicker);
        }));

        btnEraser.setOnClickListener(v -> selectTool(btnEraser, selectedTool, "Eraser", btnBrush, btnLine, btnFill));
        btnLine.setOnClickListener(v -> selectTool(btnLine, selectedTool, "Line", btnBrush, btnEraser, btnFill));
        btnFill.setOnClickListener(v -> selectTool(btnFill, selectedTool, "Fill", btnBrush, btnEraser, btnLine));
        btnBrush.setOnClickListener(v -> selectTool(btnBrush, selectedTool, "Brush", btnEraser, btnLine, btnFill));

        btnApply.setOnClickListener(v -> {
            listener.onSettingsSelected(selectedColor[0], seekBarSize.getProgress(), selectedTool[0]);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        updateRecentColors(recentColorsLayout, context, selectedColor, btnColorPicker);
        dialog.show();
    }

    private static void selectTool(Button selectedButton, String[] selectedTool, String tool, Button... otherButtons) {
        selectedTool[0] = tool;
        selectedButton.setAlpha(1.0f);
        for (Button button : otherButtons) {
            button.setAlpha(0.5f);
        }
    }

    private static void highlightSelectedTool(String tool, Button btnBrush, Button btnEraser, Button btnLine, Button btnFill) {
        btnBrush.setAlpha(tool.equals("Brush") ? 1.0f : 0.5f);
        btnEraser.setAlpha(tool.equals("Eraser") ? 1.0f : 0.5f);
        btnLine.setAlpha(tool.equals("Line") ? 1.0f : 0.5f);
        btnFill.setAlpha(tool.equals("Fill") ? 1.0f : 0.5f);
    }

    private static void addRecentColor(int color, LinearLayout layout, Context context, int[] selectedColor, Button btnColorPicker) {
        if (!recentColors.contains(color)) {
            if (recentColors.size() >= 5) {
                recentColors.remove(0);
            }
            recentColors.add(color);
        }
        updateRecentColors(layout, context, selectedColor, btnColorPicker);
    }

    private static void updateRecentColors(LinearLayout layout, Context context, int[] selectedColor, Button btnColorPicker) {
        layout.removeAllViews();
        Button lastSelectedButton = null;

        for (int color : recentColors) {
            Button colorButton = new Button(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
            params.setMargins(10, 10, 10, 10);
            colorButton.setLayoutParams(params);

            GradientDrawable colorDrawable = new GradientDrawable();
            colorDrawable.setColor(color);
            colorDrawable.setCornerRadius(20);
            colorButton.setBackground(colorDrawable);

            if (color == selectedColor[0]) {
                lastSelectedButton = colorButton;
            }

            colorButton.setOnClickListener(v -> {
                animateColorTransition(btnColorPicker, selectedColor[0], color);
                selectedColor[0] = color;
                highlightSelectedColor(colorButton, layout);
            });

            layout.addView(colorButton);
        }

        if (lastSelectedButton != null) {
            highlightSelectedColor(lastSelectedButton, layout);
        }
    }


    private static void highlightSelectedColor(Button selectedButton, LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof Button) {
                GradientDrawable normalDrawable = new GradientDrawable();
                normalDrawable.setColor(((GradientDrawable) child.getBackground()).getColor());
                normalDrawable.setCornerRadius(20);
                child.setBackground(normalDrawable);
            }
        }

        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setColor(((GradientDrawable) selectedButton.getBackground()).getColor());
        borderDrawable.setStroke(5, Color.WHITE);
        borderDrawable.setCornerRadius(20);
        selectedButton.setBackground(borderDrawable);
    }

    private static void animateColorTransition(Button button, int oldColor, int newColor) {
        button.setBackgroundColor(newColor);
        button.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).withEndAction(() ->
                        button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150)).start();
        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
        colorAnimator.setDuration(300);
        colorAnimator.addUpdateListener(animator -> button.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimator.start();
    }

    private static void updateButtonColor(Button button, int color) {
        button.setBackgroundColor(color);
    }
}
