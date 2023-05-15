package com.project.taxiappproject.objects;

import com.google.firebase.database.DataSnapshot;

public class DriverObject {

    private String id = "",
            name = "",
            phone = "",
            car = "";

    private Boolean connect = false;

    private LocationObject mLocation;

    private Boolean active = true;

    public DriverObject(String id) {
        this.id = id;
    }

    /**
     * DriverObject constructor
     * Creates an empty object
     */
    public DriverObject() {}


    /**
     * Parse datasnapshot into this object
     *
     * @param dataSnapshot - customer info fetched from the database
     */
    public void parseData(DataSnapshot dataSnapshot) {

        id = dataSnapshot.getKey();

        if (dataSnapshot.child("name").getValue() != null) {
            name = dataSnapshot.child("name").getValue().toString();
        }
        if (dataSnapshot.child("phone").getValue() != null) {
            phone = dataSnapshot.child("phone").getValue().toString();
        }
        if (dataSnapshot.child("car").getValue() != null) {
            car = dataSnapshot.child("car").getValue().toString();
        }
        if (dataSnapshot.child("activated").getValue() != null) {
            active = Boolean.parseBoolean(dataSnapshot.child("activated").getValue().toString());
        }
        if (dataSnapshot.child("connect_set").getValue() != null) {
            connect = Boolean.parseBoolean(dataSnapshot.child("connect_set").getValue().toString());
        }
        int ratingSum = 0;
        float ratingsTotal = 0;
        for (DataSnapshot child : dataSnapshot.child("rating").getChildren()) {
            ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
            ratingsTotal++;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCar() {
        return car;
    }

    public String getNameDash() {
        if(name.isEmpty()){
            return "--";
        }
        return name;
    }
    public String getPhoneDash() {
        if(phone.isEmpty()){
            return "--";
        }
        return phone;
    }
    public String getCarDash() {
        if(car.isEmpty()){
            return "--";
        }
        return car;
    }

    public void setCar(String car) {
        this.car = car;
    }

    public LocationObject getLocation() {
        return mLocation;
    }

    public void setLocation(LocationObject mLocation) {
        this.mLocation = mLocation;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getConnect() {
        return connect;
    }
}
