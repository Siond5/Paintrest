package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnMainSignUp, btnMainLogIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViews();
        btnMainSignUp.setOnClickListener(this);
        btnMainLogIn.setOnClickListener(this);
        FirebaseAuth fbAuth=FirebaseAuth.getInstance();
        if(fbAuth.getCurrentUser()!=null)
        {
            Intent intent=new Intent(MainActivity.this, MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        }

    }

    private void findViews() {
        btnMainSignUp = findViewById(R.id.btnMainSignUp);
        btnMainLogIn = findViewById(R.id.btnMainLogIn);
    }

    @Override
    public void onClick(View view) {
        if (view == btnMainSignUp) {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        } else if (view == btnMainLogIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);

        }
    }
}

