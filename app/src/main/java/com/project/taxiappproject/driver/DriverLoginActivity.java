package com.project.taxiappproject.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.taxiappproject.R;
import com.project.taxiappproject.StartingActivity;

public class DriverLoginActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText mEmail,mPassword;
    private Button mLogin,mRegister;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);
        //set buttons and textEdits
        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mLogin = (Button) findViewById(R.id.login);
        mRegister = (Button) findViewById(R.id.register);
        mLogin.setOnClickListener(this);
        mRegister.setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();
        authStateListener =  new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    @Override
    public void onClick(View v) {
         if(v.getId()==R.id.login) {
             final String email = mEmail.getText().toString();
             final String password = mPassword.getText().toString();
             if(email.matches(""))
                 mEmail.setError("Email field is required");
             else if (password.matches(""))
                 mPassword.setError("Password field is required");
             else
                 mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(DriverLoginActivity.this,
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()) {
                                Toast.makeText(DriverLoginActivity.this,"An error occurred",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                String userId = mAuth.getCurrentUser().getUid();
                                DatabaseReference currentUserDb = FirebaseDatabase.getInstance()
                                        .getReference().child("Users").child("Drivers").child(userId);
                            }
                        }
                    });
        } else {
             Intent intent = new Intent(DriverLoginActivity.this, DriverRegistrationActivity.class);
             startActivity(intent);
             finish();
         }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(DriverLoginActivity.this, StartingActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(authStateListener);
    }
}