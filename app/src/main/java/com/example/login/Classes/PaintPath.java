package com.example.login.Classes;

import android.graphics.Bitmap;
import android.graphics.Path;
import java.util.ArrayList;
import java.util.List;

public class PaintPath {
    public Path path;
    public int color;
    public float strokeWidth;
    public String tool; // e.g., "Brush", "Eraser", "Line", "fill", "clear"
    public List<PaintPath> backup;         // Used for global actions (clear/fill)
    public Bitmap bitmapSnapshot;          // Used for fill actions

    public PaintPath(Path path, int color, float strokeWidth, String tool) {
        // Create a copy of the path.
        this.path = new Path(path);
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.tool = tool;
        this.backup = null;
        this.bitmapSnapshot = null;
    }

    // Copy constructor for deep copying.
    public PaintPath(PaintPath other) {
        this.path = new Path(other.path);
        this.color = other.color;
        this.strokeWidth = other.strokeWidth;
        this.tool = other.tool;
        if (other.backup != null) {
            this.backup = new ArrayList<>();
            for (PaintPath p : other.backup) {
                this.backup.add(new PaintPath(p));
            }
        } else {
            this.backup = null;
        }
        // For bitmapSnapshot, a shallow copy is used (adjust if needed).
        this.bitmapSnapshot = other.bitmapSnapshot;
    }
}
