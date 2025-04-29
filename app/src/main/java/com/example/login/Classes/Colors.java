package com.example.login.Classes;

import android.app.Activity;

import androidx.core.content.ContextCompat;

import com.example.login.R;

/**
 * Manages color configurations for background and buttons.
 */
public class Colors {
    private int backgroundColor;
    private int buttonColor;

    /**
     * Default constructor for Colors.
     */
    public Colors() {
    }

    /**
     * Constructs a Colors object and sets the background color to the default color from resources.
     *
     * @param activity The activity context used to access resources.
     */
    public Colors(Activity activity) {
        this.backgroundColor = ContextCompat.getColor(activity, R.color.Default);
    }

    /**
     * Constructs a Colors object with specified background and button colors.
     *
     * @param backgroundColor The background color.
     * @param buttonColor     The button color.
     */
    public Colors(int backgroundColor, int buttonColor) {
        this.backgroundColor = backgroundColor;
        this.buttonColor = buttonColor;
    }

    /**
     * Gets the background color.
     *
     * @return The background color.
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color.
     *
     * @param backgroundColor The background color to set.
     */
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Gets the button color.
     *
     * @return The button color.
     */
    public int getButtonColor() {
        return buttonColor;
    }

    /**
     * Sets the button color.
     *
     * @param buttonColor The button color to set.
     */
    public void setButtonColor(int buttonColor) {
        this.buttonColor = buttonColor;
    }
}