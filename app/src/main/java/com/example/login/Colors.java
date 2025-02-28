package com.example.login;

import android.app.Activity;

import androidx.core.content.ContextCompat;

public class Colors {
private int backgroundColor;
    public Colors() {
    }
    public Colors(Activity activity) {
        this.backgroundColor =ContextCompat.getColor(activity, R.color.Default);
    }

    public Colors(int backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
