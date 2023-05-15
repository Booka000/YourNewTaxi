package com.project.taxiappproject.objects;

import com.google.firebase.database.DataSnapshot;

public class CustomerObject {
    private String id = "",
            name = "",
            phone = "";

    public CustomerObject(String id) {
        this.id = id;
    }

    /**
     * CustomerObject constructor
     * Creates an empty object
     */
    public CustomerObject() {
    }


    /**
     * Parse datasnapshot into this object
     *
     * @param dataSnapshot - customer info fetched from the database
     */
    public void parseData(DataSnapshot dataSnapshot) {
        id = dataSnapshot.getKey();
        if (dataSnapshot.child("Name").getValue() != null) {
            name = dataSnapshot.child("Name").getValue().toString();
        }
        if (dataSnapshot.child("Phone").getValue() != null) {
            phone = dataSnapshot.child("Phone").getValue().toString();
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
}
