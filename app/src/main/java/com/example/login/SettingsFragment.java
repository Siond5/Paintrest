package com.example.login;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

    public class SettingsFragment extends Fragment {

        Spinner set_color;
        SharedPreferences sp;
        boolean isSettingSelection = false;  // Flag to prevent triggering onItemSelected

        public SettingsFragment() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View view = inflater.inflate(R.layout.fragment_settings, container, false);

            set_color = view.findViewById(R.id.set_color);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.color_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            set_color.setAdapter(adapter);

            sp = getActivity().getSharedPreferences("userDetails", Context.MODE_PRIVATE);
            loadColor(view); // This will load the background color

            // Set the spinner selection based on the current background color
            changeSelectionBasedOnColor(view, getActivity());

            set_color.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    // Don't trigger any action if we're setting the selection programmatically
                    if (isSettingSelection) {
                        isSettingSelection = false;
                        return;
                    }

                    changeColorBasedOnSelection(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            return view;
        }

        private void changeSelectionBasedOnColor(View view, Activity activity) {
            // Get the current background color of the view
            ColorDrawable colorDrawable = (ColorDrawable) view.getBackground();
            int currentColor = colorDrawable.getColor();

            // Define an array of predefined colors to compare against
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

            // Loop through the color array and check for a match
            for (int i = 0; i < colorArray.length; i++) {
                if (currentColor == ContextCompat.getColor(activity, colorArray[i])) {
                    // Temporarily disable the listener to prevent it from being triggered
                    isSettingSelection = true;
                    set_color.setSelection(i);
                    return; // Exit after selecting the matching color
                }
            }

            // If no match is found, you can set a default value (e.g., the "color picker" option)
            isSettingSelection = true;
            set_color.setSelection(10);
        }

        private void changeColorBasedOnSelection(int position) {
            int color = sp.getInt("color", R.color.Default);
            switch (position) {
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
                    // Open color picker dialog if option 10 is selected
                    ColorPickerDialog.show(getActivity(), color, selectedColor -> {
                        setBackgroundColor(selectedColor);
                    });
                    return; // Ensure no further actions are taken for color picker
                default:
                    color = R.color.Default;
            }

            // Only change the background if the color is different and it's not the color picker
            ColorDrawable colorDrawable = (ColorDrawable) getView().getBackground();
            int currentColor = colorDrawable.getColor();
            if (color != currentColor && position != 10) {
                setBackgroundColor(color);
            }
        }

        private void setBackgroundColor(int color) {
            getView().setBackgroundColor(color);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("color", color);
            editor.apply();

            Toast.makeText(getActivity(), "Color Changed", Toast.LENGTH_SHORT).show();

            Colors color1 = new Colors(color);
            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            FirebaseFirestore store = FirebaseFirestore.getInstance();
            store.collection("colors")
                    .document(fbAuth.getCurrentUser().getUid()).set(color1);
        }

        public void loadColor(View view) {
            int color = sp.getInt("color", R.color.Default);
            view.setBackgroundColor(color);
        }

        public int getContextColor(int color) {
            return ContextCompat.getColor(getActivity(), color);
        }
    }