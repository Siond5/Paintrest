package com.example.login.Activities;

import android.content.Context;
import android.content.Intent;
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

public class UiSettingsActivity extends AppCompatActivity {

    Spinner set_color;
    SharedPreferences sp;
    ImageView btnReturn;
    View view;
    // We'll store the color array with a placeholder as the first element.
    String[] colorsWithPlaceholder;
    ArrayAdapter<String> adapter; // keep reference to adapter

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
        set_color = findViewById(R.id.set_color);
        btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(v -> finish());

        String[] originalColors = getResources().getStringArray(R.array.color_array);
        // Create the placeholder array with one extra slot.
        colorsWithPlaceholder = new String[originalColors.length + 1];
        // Initially, set the first element to a default hex value (e.g. white).
        colorsWithPlaceholder[0] = " ";
        System.arraycopy(originalColors, 0, colorsWithPlaceholder, 1, originalColors.length);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colorsWithPlaceholder);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        set_color.setAdapter(adapter);

        sp = this.getSharedPreferences("userDetails", Context.MODE_PRIVATE);

        loadColor();
        changeSelectionBasedOnColor();

        set_color.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // If the placeholder (position 0) is selected, do nothing.
                if (position == 0) return;
                changeColorBasedOnSelection(position);
                // After processing, reset selection back to placeholder so the same option can be chosen later.
                //set_color.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void changeSelectionBasedOnColor() {
        ColorDrawable colorDrawable = (ColorDrawable) view.getBackground();
        int currentColor = colorDrawable.getColor();

        int[] colorArray = {
                R.color.Default,
                R.color.white,
                R.color.red,
                R.color.orange,
                R.color.yellow,
                R.color.green,
                R.color.blue,
                R.color.azure,
                R.color.purple,
                R.color.pink
        };

        
        for (int i = 0; i < colorArray.length; i++) {
            if (currentColor == ContextCompat.getColor(this, colorArray[i])) {
                colorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & currentColor));                set_color.setSelection(i + 1);
                return;
            }
        }
            colorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & currentColor));
            set_color.setSelection(0);

    }

    private void changeColorBasedOnSelection(int position) {
        // Adjust position because position 0 is the placeholder.
        int adjustedPosition = position - 1;
        int color = sp.getInt("color", R.color.Default);
        switch (adjustedPosition) {
            case 0:
                color = getContextColor(R.color.Default);
                break;
            case 1:
                color = getContextColor(R.color.white);
                break;
            case 2:
                color = getContextColor(R.color.red);
                break;
            case 3:
                color = getContextColor(R.color.orange);
                break;
            case 4:
                color = getContextColor(R.color.yellow);
                break;
            case 5:
                color = getContextColor(R.color.green);
                break;
            case 6:
                color = getContextColor(R.color.blue);
                break;
            case 7:
                color = getContextColor(R.color.azure);
                break;
            case 8:
                color = getContextColor(R.color.purple);
                break;
            case 9:
                color = getContextColor(R.color.pink);
                break;
            case 10:
                // Show the custom color picker dialog.
                ColorPickerDialog.show(this, color, selectedColor -> {
                    setBackgroundColor(selectedColor);
                    // Update the placeholder to show the custom hex value.
                    colorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & selectedColor));
                    adapter.notifyDataSetChanged();
                    // Set spinner selection to the updated placeholder.
                    set_color.setSelection(0);
                });
                return;
            default:
                color = getContextColor(R.color.Default);
        }

        ColorDrawable colorDrawable = (ColorDrawable) view.getBackground();
        int currentColor = colorDrawable.getColor();
        colorsWithPlaceholder[0] = String.format("#%06X", (0xFFFFFF & color));
        if (color != currentColor) {
            setBackgroundColor(color);
        }
    }

    private void setBackgroundColor(int color) {
        view.setBackgroundColor(color);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("color", color);
        editor.apply();

        Toast.makeText(this, "Color Changed", Toast.LENGTH_SHORT).show();

        Colors color1 = new Colors(color);
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        store.collection("colors")
                .document(fbAuth.getCurrentUser().getUid()).set(color1);
    }

    public void loadColor() {
        int color = sp.getInt("color", R.color.Default);
        view.setBackgroundColor(color);
    }

    public int getContextColor(int color) {
        return ContextCompat.getColor(this, color);
    }
}
