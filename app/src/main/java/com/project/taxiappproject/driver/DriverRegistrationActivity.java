package com.project.taxiappproject.driver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.taxiappproject.R;
import com.project.taxiappproject.customer.CustomerMapActivity;

import java.util.HashMap;
import java.util.Map;

public class DriverRegistrationActivity extends AppCompatActivity implements View.OnClickListener {

    EditText mName, mEmail, mPassword, mPhone,mCarModel;
    Button mRegister,mBack;
    FirebaseAuth auth;
    FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_registration);
        mName = (EditText) findViewById(R.id.customerName);
        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mPhone = (EditText) findViewById(R.id.phoneNumber);
        mCarModel = (EditText) findViewById(R.id.car);
        mRegister = (Button) findViewById(R.id.register);
        mBack = (Button) findViewById(R.id.back);

        mRegister.setOnClickListener(this);
        mBack.setOnClickListener(this);

        auth = FirebaseAuth.getInstance();
        authStateListener =  new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    Intent intent = new Intent(DriverRegistrationActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.register) {
            final String name = mName.getText().toString();
            final String email = mEmail.getText().toString();
            final String phone = mPhone.getText().toString();
            final String password = mPassword.getText().toString();
            final String carModel = mCarModel.getText().toString();
            if (name.matches("")) mName.setError("Please write your name!");
            else if (email.matches("")) mEmail.setError("Please write your email!");
            else if (password.matches("")) mPassword.setError("Please write your name!");
            else if (phone.matches("")) mPhone.setError("Please write your name!");
            else {
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(DriverRegistrationActivity.this,
                                new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (!task.isSuccessful()) {
                                            Toast.makeText(DriverRegistrationActivity.this, "An error occurred",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            String userId = auth.getCurrentUser().getUid();
                                            DatabaseReference currentUserDb = FirebaseDatabase.getInstance()
                                                    .getReference().child("Users").child("Drivers").child(userId);
                                            currentUserDb.setValue(true);
                                            Map info = new HashMap();
                                            info.put("Name", name);
                                            info.put("Phone", phone);
                                            info.put("Car",carModel);
                                            currentUserDb.updateChildren(info);
                                        }
                                    }
                                });
            }
        } else {
            Intent intent = new Intent(DriverRegistrationActivity.this, DriverLoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(DriverRegistrationActivity.this, DriverLoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(authStateListener);
    }
}