package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    EditText etLoginEmail, etLoginPassword;
    Button btnLogin ,goSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViews();
        btnLogin.setOnClickListener(this);
        goSignup.setOnClickListener(this);
    }

    private void findViews() {
        etLoginEmail=findViewById(R.id.etLoginEmail);
        etLoginPassword=findViewById(R.id.etLoginPassword);
        btnLogin=findViewById(R.id.btnLogin);
        goSignup=findViewById(R.id.goSignup);
    }

    @Override
    public void onClick(View view) {
        if(view==goSignup)
        {
            Intent intent=new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        }
        else if (view==btnLogin){
            String email=etLoginEmail.getText().toString();
            String pass=etLoginPassword.getText().toString();
            FirebaseAuth fbAuth=FirebaseAuth.getInstance();
            fbAuth.signInWithEmailAndPassword(email,pass)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Intent intent=new Intent(LoginActivity.this, MenuActivity.class);
                            startActivity(intent);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginActivity.this, "ERROR: email or password incorrect", Toast.LENGTH_SHORT).show();
                        }
                    });

        }
    }

}