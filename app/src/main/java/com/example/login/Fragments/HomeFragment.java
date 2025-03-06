package com.example.login.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.example.login.R;

public class HomeFragment extends Fragment {

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        loadColor(getActivity(), view);

        return view;
    }

    public void loadColor(Activity activity, View view){
        sharedPreferences = activity.getSharedPreferences("userDetails", Context.MODE_PRIVATE);
        int color = sharedPreferences.getInt("color", R.color.Default);
        view.setBackgroundColor(color);
    }
}
