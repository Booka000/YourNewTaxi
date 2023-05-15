package com.project.taxiappproject.tests;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.project.taxiappproject.R;


public class TestActivity extends AppCompatActivity implements View.OnClickListener,OnDataChangeListener{
    TextView textView;
    Button button1,button2;
    int value;
    IntegerTest integerTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        value=0;
        integerTest = new IntegerTest(getApplicationContext(),null);
        button1 = (Button) findViewById(R.id.change_value);
        button2 = (Button) findViewById(R.id.reset);
        textView = (TextView) findViewById(R.id.text_field);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.change_value:
                value++;
                integerTest.setValue(value);
                break;
            case R.id.reset:
                value = 0;
                integerTest.reset();
                break;
        }
    }

    @Override
    public void onValueChange(int value) {
        textView.setText(String.valueOf(value));
    }

    @Override
    public void onReset() {
        textView.setText(String.valueOf(0));
    }

    @Override
    protected void onStart() {
        super.onStart();
        integerTest.addDataChangeListener(this);
    }
}