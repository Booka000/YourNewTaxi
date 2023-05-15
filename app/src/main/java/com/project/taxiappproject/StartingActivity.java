package com.project.taxiappproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.project.taxiappproject.customer.CustomerLoginActivity;
import com.project.taxiappproject.customer.CustomerRegistrationActivity;
import com.project.taxiappproject.driver.DriverLoginActivity;


public class StartingActivity extends AppCompatActivity implements View.OnClickListener {

    Button button1,button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting);

        button1 = (Button) findViewById(R.id.customerLogin);
        button2 = (Button) findViewById(R.id.driverLogin);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.customerLogin){
            Intent intent =  new Intent(StartingActivity.this, CustomerLoginActivity.class);
            startActivity(intent);
        } else {
            Intent intent =  new Intent(StartingActivity.this, DriverLoginActivity.class);
            startActivity(intent);
        }
    }
}