package com.project.taxiappproject.driver;

import static android.widget.Toast.makeText;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.taxiappproject.R;
import com.project.taxiappproject.databinding.ActivityDriverMapBinding;
import com.project.taxiappproject.objects.CustomerObject;
import com.project.taxiappproject.objects.RideObject;

import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    final int REQUEST_LOCATION_PERMISSION_CODE = 202210;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    Location currentLocation;
    private ActivityDriverMapBinding binding;
    Button mLogoutButton,cancelRideButton,customerPickedUp,finishRideButton;
    Boolean isLoggingOut = false;
    Switch connectionSwitch;
    Boolean Busy = false;
    DatabaseReference rideStateRef;
    ValueEventListener rideStateRefListener;
    DatabaseReference assignedClientRef;
    ValueEventListener assignedClientRefListener;
    Marker rideMarker;
    CustomerObject currentCustomer;
    RideObject currentRide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest.Builder(2000)
                .setMinUpdateIntervalMillis(1000)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY).build();
        mLogoutButton = (Button) findViewById(R.id.Logout);
        mLogoutButton.setOnClickListener(this);
        connectionSwitch = (Switch) findViewById(R.id.working_mode);
        connectionSwitch.setOnClickListener(this);
        cancelRideButton = (Button) findViewById(R.id.cancel_button);
        cancelRideButton.setOnClickListener(this);
        customerPickedUp = (Button) findViewById(R.id.picked_up_customer);
        customerPickedUp.setOnClickListener(this);
        finishRideButton = (Button) findViewById(R.id.finish_ride);
        finishRideButton.setOnClickListener(this);

    }

    LocationCallback mLocationCallBack = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                GeoFire geoFireWorking = new GeoFire(refWorking);
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                GeoFire geoFireAvailable = new GeoFire(refAvailable);
                currentLocation = location;
                if(Busy){
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), (key, error) -> {
                        if(key == null){
                            Log.d("Database error",error.toString());
                        }
                    });
                } else {
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), (key, error) -> {
                        if(key == null){
                            Log.d("Database error",error.toString());
                        }
                    });
                }

            }
        }
    };

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                checkLocationPermission();
            }

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                new android.app.AlertDialog.Builder(this).setTitle("give permission").setMessage(
                        "give permission message").setPositiveButton("OK",
                        (dialogInterface, i) -> ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE)).create().show();
            } else {
                ActivityCompat.requestPermissions(DriverMapActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CALL_PHONE}, REQUEST_LOCATION_PERMISSION_CODE);
            }
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
            }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isLoggingOut){
            disconnectDriver();
        }
    }

    private void disconnectDriver(){
        if(!Busy) {
            if(assignedClientRefListener != null)
                assignedClientRef.removeEventListener(assignedClientRefListener);
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallBack);
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversAvailable");
            GeoFire geoFireWorking = new GeoFire(refWorking);
            geoFireWorking.removeLocation(userId);
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.Logout){
            if(!Busy){
            isLoggingOut = true;
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallBack);
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(DriverMapActivity.this, DriverLoginActivity.class);
            startActivity(intent);
            finish();
            } else {
                makeText(getApplicationContext(), "Can't logout while having an order", Toast.LENGTH_LONG).show();
            }
        } else if(v.getId()==R.id.working_mode){
            if(connectionSwitch.isChecked()) {
                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                listenToAClient();
            } else
                if(!Busy){
                disconnectDriver();
                } else {
                    connectionSwitch.setChecked(true);
                    Toast.makeText(DriverMapActivity.this,"You can't disconnect while having an order"
                    ,Toast.LENGTH_SHORT).show();
                }
        } else if (v.getId() == R.id.cancel_button) {
            currentRide.cancelRide();
            cancelOrFinishRide();
        } else if (v.getId() == R.id.picked_up_customer){
            double dis = distance(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),
                    currentRide.getPickup().getCoordinates());
            if(dis <= 100.0) {
                currentRide.pickedCustomer();
                customerPickedUp.setVisibility(View.GONE);
                finishRideButton.setVisibility(View.VISIBLE);
                rideMarker.setPosition(currentRide.getDestination().getCoordinates());
                } else {
                Toast.makeText(getApplicationContext(),"Get closer to the client first",Toast.LENGTH_SHORT).show();
            }
        } else {
            double dis = distance(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),
                    currentRide.getDestination().getCoordinates());
            if(dis <= 100.0) {
                currentRide.finishedRide();
                cancelOrFinishRide();
            } else {
                Toast.makeText(getApplicationContext(),"Get closer to the destination first",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void listenToAClient(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        assignedClientRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Drivers").child(userId).child("currentRideId");
        assignedClientRefListener = assignedClientRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String id = snapshot.getValue(String.class);
                    checkNewOrder(id);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void cancelOrFinishRide(){
        Busy = false;
        deleteRideIdChild();
        if(rideMarker != null)
            rideMarker.remove();
        if(rideStateRefListener != null)
            rideStateRef.removeEventListener(rideStateRefListener);
        cancelRideButton.setVisibility(View.GONE);
        customerPickedUp.setVisibility(View.GONE);
        finishRideButton.setVisibility(View.GONE);
        currentCustomer = null;
    }

    private void checkNewOrder(String id){

        DatabaseReference RideRef = FirebaseDatabase.getInstance().getReference()
                .child("ride_info").child(id);
        RideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    RideObject newRide = new RideObject(id);
                    newRide.parseData(snapshot);
                    alertDriver(newRide);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void alertDriver(RideObject rideObject){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        TextView Pickup = dialog.findViewById(R.id.pickup_dialog);
        TextView destination = dialog.findViewById(R.id.destination_dialog);
        TextView distance = dialog.findViewById(R.id.distance_dialog);
        //TextView price = dialog.findViewById(R.id.pickup_dialog);

        Pickup.setText(rideObject.getPickup().getName());
        destination.setText(rideObject.getDestination().getName());
        distance.setText(rideObject.getCalculatedRideDistance());

        Button acceptButton =  dialog.findViewById(R.id.accept);
        Button rejectButton = dialog.findViewById(R.id.Reject);
         acceptButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 dialog.dismiss();
                 Busy = true;
                 currentRide = rideObject;
                 cancelRideButton.setVisibility(View.VISIBLE);
                 customerPickedUp.setVisibility(View.VISIBLE);
                 listenToRideState();
                 getAssignedCustomerInfo();
                 currentRide.confirmDriver();
                 getAssignedCustomerPickupLocation();
             }
         });

         rejectButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 dialog.dismiss();
                 deleteRideIdChild();

             }
         });

         dialog.setOnShowListener(new DialogInterface.OnShowListener() {
             private static final int AUTO_DISMISS_MILLIS = 8000;
             @Override
             public void onShow(DialogInterface dialog) {
                 new CountDownTimer(AUTO_DISMISS_MILLIS, 1000) {
                     @Override
                     public void onTick(long millisUntilFinished) {
                         rejectButton.setText(String.format(
                                 Locale.getDefault(), "%s (%d)",
                                 "Reject",
                                 TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1));
                     }
                     @Override
                     public void onFinish() {
                         if (((Dialog) dialog).isShowing()) {
                             dialog.dismiss();
                             deleteRideIdChild();
                         }
                     }
                 }.start();
             }
         });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);

    }

    private void getAssignedCustomerPickupLocation(){
        LatLng latLng = currentRide.getPickup().getCoordinates();
        rideMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Pickup here"));
    }

    private void deleteRideIdChild(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedClientRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Drivers").child(userId).child("currentRideId");
        assignedClientRef.setValue(null);
    }

    private void getAssignedCustomerInfo() {
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Customers").child(currentRide.getCustomer().getId());
        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    String key = snapshot.getKey().toString();
                    Toast.makeText(DriverMapActivity.this, key, Toast.LENGTH_SHORT).show();
                    CustomerObject customerObject = new CustomerObject(currentRide.getCustomer().getId());
                    customerObject.parseData(snapshot);
                    currentRide.setCustomer(customerObject);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private double distance(LatLng current,LatLng destination) {

        final int R = 6371;

        double latDistance = Math.toRadians(destination.latitude - current.latitude);
        double lonDistance = Math.toRadians(destination.longitude - current.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(current.latitude)) * Math.cos(Math.toRadians(destination.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000;
        distance = Math.pow(distance, 2) + Math.pow(0.0, 2);

        return Math.sqrt(distance);
    }

    private void alertCanceledOrFinished(){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet);

        Button ok =  dialog.findViewById(R.id.ok);
        TextView statement = dialog.findViewById(R.id.statement);
        if(currentRide.getState() == 3){
            statement.setText("The Ride was finished :)");
        } else if (currentRide.getState() ==-1){
            statement.setText("The client has canceled :(");
        }

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void listenToRideState(){
        rideStateRef = FirebaseDatabase.getInstance().getReference()
                .child("ride_info").child(currentRide.getId()).child("state");
        rideStateRefListener = rideStateRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    currentRide.setState(-1);
                    cancelOrFinishRide();
                    alertCanceledOrFinished();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}