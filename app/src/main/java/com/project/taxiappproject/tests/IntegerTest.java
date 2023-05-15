package com.project.taxiappproject.tests;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class IntegerTest extends Worker {

    private int value;
    private OnDataChangeListener dataChangeListener;

    public IntegerTest(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        value = 0;
    }


    public void addDataChangeListener(OnDataChangeListener dataChangeListener){
        this.dataChangeListener = dataChangeListener;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        if (dataChangeListener != null) {
            dataChangeListener.onValueChange(value);
        }
    }

    public void reset () {
        value = 0;
        if (dataChangeListener != null) {
            dataChangeListener.onReset();
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        return null;
    }
}
