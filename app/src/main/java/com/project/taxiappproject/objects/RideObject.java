package com.project.taxiappproject.objects;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RideObject {
    private String id;
    private float rideDistance = 0;

    private LocationObject pickup, destination;
    DatabaseReference rideRef;

    private DriverObject mDriver;
    private CustomerObject mCustomer;

    Boolean ended = false;

    private double duration, distance, estimatedPrice;
    private float calculatedRideDistance, timePassed = 0;

    private int state;

    /**
     * RideObject constructor
     * @param id - id of the ride
     */
    public RideObject( String id){
        this.id = id;
    }

    /**
     * RideObject constructor
     *
     * creates an empty object
     */
    public RideObject(){}

    /**
     * Checks if user is able to place a ride request
     * @return -1 if not able and 0 otherwise
     */
    public Boolean checkRide(){
        if (destination == null) {
            return false;
        }
        if (pickup == null) {
            return false;
        }

        return true;
    }

    public Boolean checkRideReady(){
        if (destination == null) {
            return false;
        }
        if (pickup == null) {
            return false;
        }

        return true;
    }



    /**
     * Parse the Datasnapshot retrieved from the database and puts it into a RideObject
     * @param dataSnapshot - datasnapshot of the ride
     */
    public void parseData(DataSnapshot dataSnapshot){
        id = dataSnapshot.getKey();

        pickup = new LocationObject();
        destination = new LocationObject();

        if(dataSnapshot.child("pickup").child("name").getValue()!=null){
            pickup.setName(dataSnapshot.child("pickup").child("name").getValue().toString());
        }
        if(dataSnapshot.child("destination").child("name").getValue()!=null){
            destination.setName(dataSnapshot.child("destination").child("name").getValue().toString());
        }
        if(dataSnapshot.child("pickup").child("lat").getValue()!=null && dataSnapshot.child("pickup").child("lng").getValue()!=null){
            pickup.setCoordinates(
                    new LatLng(Double.parseDouble(dataSnapshot.child("pickup").child("lat").getValue().toString()),
                            Double.parseDouble(dataSnapshot.child("pickup").child("lng").getValue().toString())));
        }
        if(dataSnapshot.child("destination").child("lat").getValue()!=null && dataSnapshot.child("destination").child("lng").getValue()!=null){
            destination.setCoordinates(
                    new LatLng(Double.parseDouble(dataSnapshot.child("destination").child("lat").getValue().toString()),
                            Double.parseDouble(dataSnapshot.child("destination").child("lng").getValue().toString())));
        }

        if(dataSnapshot.child("customerId").getValue() != null){
            mCustomer = new CustomerObject(dataSnapshot.child("customerId").getValue().toString());
        }
        if(dataSnapshot.child("driverId").getValue() != null){
            mDriver = new DriverObject(dataSnapshot.child("driverId").getValue().toString());
        }
        if(dataSnapshot.child("ended").getValue() != null){
            ended = Boolean.parseBoolean(dataSnapshot.child("ended").getValue().toString());
        }
        if(dataSnapshot.child("state").getValue() != null){
            state = Integer.parseInt(dataSnapshot.child("state").getValue().toString());
        }

        if(dataSnapshot.child("calculated_duration").getValue() != null){
            duration = Double.parseDouble(dataSnapshot.child("calculated_duration").getValue().toString());
        }
        if(dataSnapshot.child("calculated_distance").getValue() != null){
            distance = Double.parseDouble(dataSnapshot.child("calculated_distance").getValue().toString());
        }
        if(dataSnapshot.child("calculated_Price").getValue() != null){
            estimatedPrice = Double.parseDouble(dataSnapshot.child("calculated_Price").getValue().toString());
        }

        rideRef = FirebaseDatabase.getInstance().getReference().child("ride_info").child(this.id);

        //Calculate the ride distance, by calculating the direct distance between pickup and destination
        if(this.pickup.getCoordinates() != null && this.destination.getCoordinates() != null){
            Location loc1 = new Location("");
            loc1.setLatitude(this.pickup.getCoordinates().latitude);
            loc1.setLongitude(this.pickup.getCoordinates().longitude);

            Location loc2 = new Location("");
            loc2.setLatitude(this.destination.getCoordinates().latitude);
            loc2.setLongitude(this.destination.getCoordinates().longitude);

            calculatedRideDistance = loc1.distanceTo(loc2) / 1000;
        }
    }

    /**
     * Post Ride info to the database, this write action will be triggered by the
     * firebase functions and only then will it be available to the drivers.
     */
    public void postRideInfo(){
        rideRef = FirebaseDatabase.getInstance().getReference().child("ride_info");

        id =  rideRef.push().getKey();
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        HashMap map = new HashMap();
        map.put("state", 0);
        map.put("customerId", customerId);
        map.put("ended", false);
       // map.put("calculated_duration",);
        map.put("calculated_distance", distance()/1000);
        //map.put("calculated_Price", Utils.rideCostEstimate(distance, duration));
        map.put("destination/name", destination.getName());
        map.put("destination/lat", destination.getCoordinates().latitude);
        map.put("destination/lng", destination.getCoordinates().longitude);
        map.put("pickup/name", pickup.getName());
        map.put("pickup/lat", pickup.getCoordinates().latitude);
        map.put("pickup/lng", pickup.getCoordinates().longitude);

        rideRef.child(id).updateChildren(map);
        rideRef = rideRef.child(id);
    }

    private double distance() {

        final int R = 6371;

        double latDistance = Math.toRadians(destination.getCoordinates().latitude - pickup.getCoordinates().latitude);
        double lonDistance = Math.toRadians(destination.getCoordinates().longitude - pickup.getCoordinates().longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(pickup.getCoordinates().latitude)) * Math.cos(Math.toRadians(destination.getCoordinates().latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
         distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2) + Math.pow(0.0, 2);

        return Math.sqrt(distance);
    }
    /**
     * When the driver has picked up the customer this function must be called in order to
     * know the state of the ride.
     */
    public void pickedCustomer(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("ride_info").child(id);

        state = 2;

        HashMap map = new HashMap();
        map.put("state", state);

        ref.updateChildren(map);
    }

    public void finishedRide(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("ride_info").child(id);

        state = 3;

        HashMap map = new HashMap();
        map.put("state", 3);

        ref.updateChildren(map);
    }
    /**
     * Called when a drive is meant to be cancelled
     */
    public void cancelRide(){
        if(id == null){return;}
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("ride_info").child(id);

        HashMap map = new HashMap();
        map.put("state", -1);
        map.put("cancelled", true);

        ref.updateChildren(map);
    }

    /**
     * Called when a driver wants to accept a customer request
     */
    public void confirmDriver(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("ride_info").child(id);

        state = 1;

        HashMap map = new HashMap();
        map.put("state", 1);
        map.put("driverId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        ref.updateChildren(map);
    }

    public DriverObject getDriver() {
        return mDriver;
    }
    public void setDriver(DriverObject mDriver) {
        this.mDriver = mDriver;
    }

    public CustomerObject getCustomer() {
        return mCustomer;
    }
    public void setCustomer(CustomerObject mCustomer) {
        this.mCustomer = mCustomer;
    }

    public LocationObject getPickup() {
        return pickup;
    }
    public void setPickup(LocationObject pickup) {
        this.pickup = pickup;
    }

    public LocationObject getDestination() {
        return destination;
    }
    public void setDestination(LocationObject destination) {
        this.destination = destination;
    }

    public float getRideDistance() {
        return rideDistance;
    }

    public void setRideDistance(float rideDistance) {
        this.rideDistance = rideDistance;
    }

    public void setTimePassed(float timePassed) {
        this.timePassed = timePassed;
    }

    public String getId() {
        return id;
    }

    public Boolean getEnded() {
        return ended;
    }

/*    @SuppressLint("DefaultLocale")
    public String getPriceString(){
        return String.format("%.2f", ridePrice);
    }*/



    public DatabaseReference getRideRef() {
        return rideRef;
    }

    public String getCalculatedRideDistance() {
        String s = String.valueOf(calculatedRideDistance);
        String s1 = s.substring(s.indexOf(".")+2).trim();
        return s.replace(s1, "") + " km";
    }

    public void setState(int state) {
        this.state = state;
    }

    public float getTimePassed() {
        return timePassed;
    }

    public int getCalculatedTime(){
        return (int) calculatedRideDistance * 100 / 60;
    }

    public int getState() {
        return state;
    }

    public double getDistance() {
        return distance;
    }

    public double getDuration() {
        return duration;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String getDurationString(){
        int days = (int) duration / 86400;
        int hours = ((int) duration - days * 86400) / 3600;
        int minutes = ((int) duration - days * 86400 - hours * 3600) / 60;
        int seconds = (int) duration - days * 86400 - hours * 3600 - minutes * 60;

        return hours + " hour " + minutes + "min";
    }

    public double getEstimatedPrice() {
        return estimatedPrice;
    }
}
