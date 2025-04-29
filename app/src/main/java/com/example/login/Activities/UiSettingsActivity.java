/**
 * Activity for UI customization settings.
 *
 * Allows the user to pick background and button colors via spinners and a color picker dialog.
 * Selected colors are applied to the view, saved in SharedPreferences, and persisted to Firestore.
 */
package com.example.login.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

import com.example.login.Classes.Colors;
import com.example.login.Dialogs.ColorPickerDialog;
import com.example.login.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Manages the UI settings screen where the user can change background and button colors.
 */
public class UiSettingsActivity extends AppCompatActivity {

    Spinner set_bgColor, set_btnColor;
    SharedPreferences sp;
    ImageView btnReturn;
    View view;
    String[] bgColorsWithPlaceholder, btnColorsWithPlaceholder;
    ArrayAdapter<String> adapter1, adapter2;

    /**
     * Initializes the activity: sets up edge-to-edge layout, view references,
     * populates spinners with color options, loads stored colors, and attaches listeners.
     *
     * @param savedInstanceState Saved instance state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ui_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        view = findViewById(R.id.main);
        set_bgColor = findViewById(R.id.set_bgColor);
        set_btnColor = findViewById(R.id.set_btnColor);
        btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(v -> finish());

        String[] originalBgColors = getResources().getStringArray(R.array.BgColor_array);
        String[] originalBtnColors = getResources().getStringArray(R.array.BtnColor_array);
        bgColorsWithPlaceholder = new String[originalBgColors.length + 1];
        btnColorsWithPlaceholder = new String[originalBtnColors.length + 1];
        bgColorsWithPlaceholder[0] = " ";
        btnColorsWithPlaceholder[0] = " ";
        System.arraycopy(originalBgColors, 0, bgColorsWithPlaceholder, 1, originalBgColors.length);
        System.arraycopy(originalBtnColors, 0, btnColorsWithPlaceholder, 1, originalBtnColors.length);

        adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bgColorsWithPlaceholder);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, btnColorsWithPlaceholder);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        set_bgColor.setAdapter(adapter1);
        set_btnColor.setAdapter(adapter2);
        sp = this.getSharedPreferences("userDetails", Context.MODE_PRIVATE);

        loadBgColor();
        changeSelectionBasedOnBgColor();
        changeSelectionBasedOnBtnColor();

        set_bgColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Handles background color spinner selection changes.
             */
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == 0) return;
                changeBgColorBasedOnSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        set_btnColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Handles button color spinner selection changes.
             */
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == 0) return;
                changeBtnColorBasedOnSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });
    }

    /**
     * Updates the placeholder and spinner selection based on the current background color of the view.
     */
    private void changeSelectionBasedOnBgColor() {
        ColorDrawable colorDrawable = (ColorDrawable) view.getBackground();
        int currentColor = colorDrawable.getColor();
        int[] colorArray = { R.color.Default, R.color.white, R.color.red, R.color.orange,
                R.color.yellow, R.color.green, R.color.blue, R.color.azure, R.color.purple, R.color.pink };
        for (int i = 0; i < colorArray.length; i++) {
            if (currentColor == ContextCompat.getColor(this, colorArray[i])) {
                bgColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & currentColor));
                set_bgColor.setSelection(i + 1);
                return;
            }
        }
        bgColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & currentColor));
        set_bgColor.setSelection(0);
    }

    /**
     * Updates the placeholder and spinner selection based on the stored button color.
     */
    private void changeSelectionBasedOnBtnColor() {
        int currentColor = sp.getInt("btnColor", R.color.button);
        int[] colorArray = { R.color.button, R.color.button2 };
        for (int i = 0; i < colorArray.length; i++) {
            if (currentColor == ContextCompat.getColor(this, colorArray[i])) {
                btnColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & currentColor));
                set_btnColor.setSelection(i + 1);
                return;
            }
        }
        btnColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & currentColor));
        set_btnColor.setSelection(0);
    }

    /**
     * Applies a new background color based on spinner selection or a custom pick.
     *
     * @param position Spinner position selected
     */
    private void changeBgColorBasedOnSelection(int position) {
        int adjustedPosition = position - 1;
        int color = sp.getInt("bGColor", R.color.Default);
        switch (adjustedPosition) {
            case 0: color = getContextColor(R.color.Default); break;
            case 1: color = getContextColor(R.color.white); break;
            case 2: color = getContextColor(R.color.red); break;
            case 3: color = getContextColor(R.color.orange); break;
            case 4: color = getContextColor(R.color.yellow); break;
            case 5: color = getContextColor(R.color.green); break;
            case 6: color = getContextColor(R.color.blue); break;
            case 7: color = getContextColor(R.color.azure); break;
            case 8: color = getContextColor(R.color.purple); break;
            case 9: color = getContextColor(R.color.pink); break;
            case 10:
                ColorPickerDialog.show(this, color, selectedColor -> {
                    setBackgroundColor(selectedColor);
                    bgColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & selectedColor));
                    adapter1.notifyDataSetChanged();
                    set_bgColor.setSelection(0);
                });
                return;
            default: color = getContextColor(R.color.Default);
        }
        bgColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & color));
        if (color != ((ColorDrawable) view.getBackground()).getColor()) {
            setBackgroundColor(color);
        }
    }

    /**
     * Applies a new button color based on spinner selection or a custom pick.
     *
     * @param position Spinner position selected
     */
    private void changeBtnColorBasedOnSelection(int position) {
        int adjustedPosition = position - 1;
        int color = sp.getInt("btnColor", R.color.Default);
        switch (adjustedPosition) {
            case 0: color = getContextColor(R.color.button); break;
            case 1: color = getContextColor(R.color.button2); break;
            case 2:
                ColorPickerDialog.show(this, color, selectedColor -> {
                    setBtnColor(selectedColor);
                    btnColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & selectedColor));
                    adapter2.notifyDataSetChanged();
                    set_btnColor.setSelection(0);
                });
                return;
            default: color = getContextColor(R.color.button);
        }
        btnColorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & color));
        if (color != sp.getInt("btnColor", R.color.button)) {
            setBtnColor(color);
        }
    }

    /**
     * Updates the view background color, saves preference, shows a toast, and persists to Firestore.
     *
     * @param bgColor The new background color integer
     */
    private void setBackgroundColor(int bgColor) {
        view.setBackgroundColor(bgColor);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("bgColor", bgColor);
        editor.apply();

        Toast.makeText(this, "Background Color Changed", Toast.LENGTH_SHORT).show();

        int btnColor = sp.getInt("btnColor", R.color.Default);
        Colors color1 = new Colors(bgColor, btnColor);
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        store.collection("colors").document(fbAuth.getCurrentUser().getUid()).set(color1);
    }

    /**
     * Updates the button color preference, shows a toast, and persists to Firestore.
     *
     * @param btnColor The new button color integer
     */
    private void setBtnColor(int btnColor) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("btnColor", btnColor);
        editor.apply();

        Toast.makeText(this, "Button Color Changed", Toast.LENGTH_SHORT).show();
        int bgColor = sp.getInt("bgColor", R.color.Default);
        Colors color1 = new Colors(bgColor, btnColor);
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        store.collection("colors").document(fbAuth.getCurrentUser().getUid()).set(color1);
    }

    /**
     * Loads the stored background color into the view on startup.
     */
    public void loadBgColor() {
        int color = sp.getInt("bgColor", R.color.Default);
        view.setBackgroundColor(color);
    }

    /**
     * Retrieves a color resource as an integer using ContextCompat.
     *
     * @param color The resource ID of the color
     * @return The resolved color integer
     */
    public int getContextColor(int color) {
        return ContextCompat.getColor(this, color);
    }
}