package com.example.login.Classes;

import android.graphics.Bitmap;
import android.graphics.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a drawable path with specific properties such as color, stroke width, and tool type.
 * Also supports backup for actions like clear/fill and bitmap snapshots for fills.
 */
public class PaintPath {
    /** The path representing the drawn shape or line. */
    public Path path;

    /** The color used to draw the path. */
    public int color;

    /** The width of the stroke used to draw the path. */
    public float strokeWidth;

    /** The tool type used (e.g., "Brush", "Eraser", "Line", "Fill", "Clear"). */
    public String tool;

    /** Backup of paths used for global actions like clear or fill. */
    public List<PaintPath> backup;

    /** Bitmap snapshot used specifically for fill actions. */
    public Bitmap bitmapSnapshot;

    /**
     * Constructs a PaintPath object with the specified path, color, stroke width, and tool.
     * Creates a deep copy of the provided path.
     *
     * @param path        The path to copy.
     * @param color       The color for the path.
     * @param strokeWidth The stroke width for the path.
     * @param tool        The tool type (e.g., "Brush", "Eraser").
     */
    public PaintPath(Path path, int color, float strokeWidth, String tool) {
        this.path = new Path(path);
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.tool = tool;
        this.backup = null;
        this.bitmapSnapshot = null;
    }

    /**
     * Copy constructor for creating a deep copy of another PaintPath object.
     *
     * @param other The PaintPath object to copy.
     */
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
        
        // Note: Bitmap snapshot is copied shallowly (points to the same bitmap).
        this.bitmapSnapshot = other.bitmapSnapshot;
    }
}