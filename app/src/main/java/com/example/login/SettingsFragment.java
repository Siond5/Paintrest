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

        sp = getActivity().getSharedPreferences("colors", Context.MODE_PRIVATE);
        loadColor(getActivity(), view);
        changeSelectionBasedOnColor(view, getActivity());

        set_color.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                changeColorBasedOnSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });

        return view;
    }

    private void changeColorBasedOnSelection(int position) {
        int color;
        switch (position) {
            case 0:
                color = R.color.Default;
                break;
            case 1:
                color = R.color.white;
                break;
            case 2:
                color = R.color.red;
                break;
            case 3:
                color = R.color.orange;
                break;
            case 4:
                color = R.color.yellow;
                break;
            case 5:
                color = R.color.green;
                break;
            case 6:
                color = R.color.blue;
                break;
                case 7:
                color = R.color.azure;
                break;
            case 8:
                color = R.color.purple;
                break;
            case 9:
                color = R.color.pink;
                break;
            default:
                color = R.color.Default;
        }
        ColorDrawable colorDrawable = (ColorDrawable) getView().getBackground();
        int color1 =  colorDrawable.getColor();
        if (ContextCompat.getColor(getActivity(), color) != color1) {
            setBackgroundColor(color);
        }
    }

    public void setBackgroundColor(int color) {
        getView().setBackgroundColor(ContextCompat.getColor(getActivity(), color));
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

    public void loadColor(Activity activity, View view){
        sp = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sp.getInt("color", R.color.Default);
        view.setBackgroundColor(ContextCompat.getColor(activity, color));
    }

    private static void changeSelectionBasedOnColor( View view, Activity activity) {
        ColorDrawable colorDrawable = (ColorDrawable) view.getBackground();
        int color =  colorDrawable.getColor();
        Spinner set_color = view.findViewById(R.id.set_color);
        int[] colorArray = {R.color.Default,R.color.white, R.color.red, R.color.orange, R.color.yellow, R.color.green, R.color.blue, R.color.azure, R.color.purple, R.color.pink};

        for (int i = 0; i < colorArray.length; i++) {
            if (color == ContextCompat.getColor(activity, colorArray[i])) {
                set_color.setSelection(i);
                return;
            }
        }

        set_color.setSelection(0);
    }
}
