package com.example.login.Classes;

import android.app.Activity;

import androidx.core.content.ContextCompat;

import com.example.login.R;

public class Colors {
private int backgroundColor;
private int buttonColor;
    public Colors() {
    }
    public Colors(Activity activity) {
        this.backgroundColor =ContextCompat.getColor(activity, R.color.Default);
    }

    public Colors(int backgroundColor , int buttonColor)
    {
        this.backgroundColor = backgroundColor;
        this.buttonColor = buttonColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getButtonColor() {
        return buttonColor;
    }

    public void setButtonColor(int buttonColor) {
        this.buttonColor = buttonColor;
    }
}
