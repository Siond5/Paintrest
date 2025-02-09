package com.example.login;

public class Colors {
private int backgroundColor;

    public Colors() {
        this.backgroundColor = R.color.Default;
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
