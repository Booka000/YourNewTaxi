package com.project.taxiappproject.customer;

import static android.widget.Toast.makeText;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.taxiappproject.R;
import com.project.taxiappproject.databinding.ActivityCustomerMapBinding;
import com.project.taxiappproject.objects.DriverObject;
import com.project.taxiappproject.objects.LocationObject;
import com.project.taxiappproject.objects.RideObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    final int REQUEST_LOCATION_PERMISSION_CODE = 202210;
    FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private ActivityCustomerMapBinding binding;
    Button mLogoutButton,mRequestARideButton,cancelRideButton;
    Boolean isLoggingOut = false;
    GeoQuery geoQuery;
    double radius = 1;
    Boolean requestBol = false;
    DatabaseReference driverLocationRef,RideStateRef;
    ValueEventListener driverLocationRefListener,RideStateRefListener;
    Marker driverMarker, rideMarker;
    RideObject currentRide;
    GoogleMap.OnCameraIdleListener onCameraIdleListener;
    ImageView pin;
    AutocompleteSupportFragment pickupAutocompleteFragment,destinationAutocompleteFragment;
    PlacesClient mPlaces;
    Stack<String> DriversKeys;
    ArrayList<String> DriversRejected;
    private BottomSheetBehavior bottomSheetBehavior;
    LinearLayout bottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomerMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLogoutButton = (Button) findViewById(R.id.Logout);
        mLogoutButton.setOnClickListener(this);
        mRequestARideButton = (Button) findViewById(R.id.requestARide);
        mRequestARideButton.setOnClickListener(this);
        cancelRideButton = findViewById(R.id.cancelARide);
        cancelRideButton.setOnClickListener(this);


        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(),getResources().getString(R.string.google_key));
        }


        DriversKeys = new Stack<String>();
        DriversRejected =  new ArrayList<String>();
        pin = (ImageView) findViewById(R.id.pickup_pin);
        mPlaces = Places.createClient(this);


        instanceAutocompletePickup();
        instanceAutocompleteDestination();
        onCameraMove();
    }

    private void onCameraMove(){
        onCameraIdleListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                try {
                    Geocoder geocoder = new Geocoder(CustomerMapActivity.this);
                    LatLng latLng = mMap.getCameraPosition().target;
                    List<Address> addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
                    String address = "";
                    if(addressList.get(0).getThoroughfare() != null && addressList.get(0).getSubThoroughfare() != null)
                        address = addressList.get(0).getThoroughfare()+ " , " + addressList.get(0).getSubThoroughfare();
                    else{
                        address = addressList.get(0).getAddressLine(0);
                    }
                    if(currentRide == null)
                        currentRide = new RideObject(null);
                    LocationObject location = new LocationObject(latLng,address);
                    Log.d("Location set",address+latLng.toString());
                    currentRide.setPickup(location);
                    final String finalAddress = address;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pickupAutocompleteFragment.setText(finalAddress);
                        }
                    });
                }catch (Exception e){
                    Log.d("Error",e.toString());
                }
            }
        };
    }

    private void instanceAutocompletePickup(){
        pickupAutocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_pickup);
        pickupAutocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG));
        pickupAutocompleteFragment.setHint("Pickup");
        pickupAutocompleteFragment.setCountry("RU");
        pickupAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.d("Error",status.toString());
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(currentRide == null)
                            currentRide = new RideObject(null);
                        LocationObject location = new LocationObject(place.getLatLng(),place.getName());
                        Log.d("Location set",place.getName()+place.getLatLng().toString());
                        currentRide.setPickup(location);
                        if(currentRide.checkRideReady()) mRequestARideButton.setClickable(true);
                    }
                }).start();
            }
        });
    }

    private void instanceAutocompleteDestination(){
        destinationAutocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_destination);
        destinationAutocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG));
        destinationAutocompleteFragment.setHint("Destination");
        destinationAutocompleteFragment.setCountry("RU");
        destinationAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.d("Error",status.toString());
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                if(currentRide == null)
                    currentRide = new RideObject(null);
                LocationObject location = new LocationObject(place.getLatLng(),place.getName());
                Log.d("Location set",location.getName()+location.getCoordinates().toString());
                currentRide.setDestination(location);
                if(currentRide.checkRideReady()) mRequestARideButton.setClickable(true);
            }
        });
    }


    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnCameraIdleListener(onCameraIdleListener);
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
                        (dialogInterface, i) -> ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE)).create().show();
            } else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this,
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

    private void disconnect(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refRequest = FirebaseDatabase.getInstance().getReference("customersRequest");
        GeoFire geoFireRequest = new GeoFire(refRequest);
        geoFireRequest.removeLocation(userId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isLoggingOut){
            disconnect();
        }
    }


    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.Logout:
                isLoggingOut = true;
                disconnect();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, CustomerLoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.requestARide:
                if(currentRide.checkRide()){
                        requestBol = true;
                        currentRide.postRideInfo();
                        mRequestARideButton.setVisibility(View.GONE);
                        pin.setVisibility(View.GONE);
                        mMap.setOnCameraIdleListener(null);
                        pickupAutocompleteFragment.setText("");
                        destinationAutocompleteFragment.setText("");
                        cancelRideButton.setVisibility(View.VISIBLE);
                        rideMarker = mMap.addMarker(new MarkerOptions().
                                position(currentRide.getPickup().getCoordinates()).title("Pick up here"));
/*                        RideHandlerThread = Executors.newSingleThreadExecutor();
                        RideHandlerThread.submit(this);*/
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            findDriversAround();
                        }
                    }).start();
                } else {
                    if(currentRide.getPickup()==null)
                        Toast.makeText(getApplicationContext(),"Choose a pick up point",Toast.LENGTH_SHORT).show();
                    else {
                        Toast.makeText(getApplicationContext(),"Choose a destination point",Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                if(requestBol) {
                    cancelRide();
                }
                break;
        }
    }

    private void cancelRide(){
        requestBol = false;
        if(geoQuery != null)
            geoQuery.removeAllListeners();
        if(driverLocationRefListener != null)
            driverLocationRef.removeEventListener(driverLocationRefListener);
        if(RideStateRefListener != null)
            RideStateRef.removeEventListener(RideStateRefListener);
        deleteRide();
        currentRide = new RideObject(null);
        radius = 1;
        DriversKeys.clear();
        DriversRejected.clear();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(rideMarker != null) rideMarker.remove();
                if(rideMarker != null) rideMarker.remove();
                if(driverMarker != null) driverMarker.remove();
                cancelRideButton.setVisibility(View.GONE);
                mRequestARideButton.setText(R.string.find_driver);
                mRequestARideButton.setVisibility(View.VISIBLE);
                pin.setVisibility(View.VISIBLE);
                mMap.setOnCameraIdleListener(onCameraIdleListener);
            }
        });
    }

    private void deleteRide(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("ride_info").child(currentRide.getId());
        ref.setValue(null);
    }

    private void findDriversAround(){
        if(requestBol) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
            GeoFire geoFire = new GeoFire(ref);
            geoQuery = geoFire.queryAtLocation(new GeoLocation(currentRide.getPickup().getCoordinates().latitude,
                    currentRide.getPickup().getCoordinates().longitude), radius);
            geoQuery.removeAllListeners();
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if (!DriversKeys.contains(key) && !DriversRejected.contains(key))
                        DriversKeys.push(key);
                }

                @Override
                public void onKeyExited(String key) {
                    if (DriversKeys.contains(key))
                        DriversKeys.remove(key);
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!DriversKeys.isEmpty() && requestBol) {
                                Log.d("Request  state", "Found " + DriversKeys.size());
                                callADriver(DriversKeys.pop());
                            } else if(!requestBol) {
                                Log.d("Request state", "request was canceled");
                            }
                            else if (radius <= 20.0){
                                Log.d("Request  state", "couldn't find any key (radius = " +radius +" )");
                                radius++;
                                findDriversAround();
                            } else {
                                Log.d("Request  state", "No drivers were found");
                                cancelRide();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        alertCanceledOrFinished();
                                    }
                                });
                            }
                        }
                    }).start();
                }
                @Override
                public void onGeoQueryError(DatabaseError error) {
                    throw error.toException(); // Don't ignore errors
                }
            });
        } else {
            Log.d("Thread state","is shutdown");
        }
    }

    private void getDriverLocation(){
        if(requestBol) {
            driverLocationRef = FirebaseDatabase.getInstance().getReference()
                    .child("driversWorking").child(currentRide.getDriver().getId()).child("l");
            driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (snapshot.exists() && requestBol) {
                                List<Object> list = (List<Object>) snapshot.getValue();
                                double LocationLat = 0, LocationLon = 0;
                                if (list.get(0) != null && list.get(0) != null) {
                                    LocationLat = Double.parseDouble(list.get(0).toString());
                                    LocationLon = Double.parseDouble(list.get(1).toString());
                                }
                                LatLng driverLatLng = new LatLng(LocationLat, LocationLon);
                                Log.d("Request State",driverLatLng.toString());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(driverMarker == null){
                                            Toast.makeText(getApplicationContext()
                                            ,"Here",Toast.LENGTH_SHORT).show();
                                            driverMarker = mMap.addMarker(new MarkerOptions()
                                                    .position(driverLatLng).title("Your driver"));
                                        } else {
                                            driverMarker.setPosition(driverLatLng);
                                        }
                                    }
                                });
                            } else if(!requestBol) {
                                Log.d("Request state", "request was canceled");
                            }
                        }
                    }).start();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }  else {
            Log.d("Thread state","is shutdown");
        }
    }

    private void callADriver(String key){
        if(requestBol) {
            DatabaseReference driverDataRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child("Drivers").child(key).child("currentRideId");
            driverDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!snapshot.exists() && requestBol) {
                                Log.d("Request  state", "The driver " + key + " is free");
                                driverDataRef.setValue(currentRide.getId());
                                checkDriverAccepted(key);
                            } else if (!requestBol){
                                Log.d("Request  state", "was canceled");
                            }
                            else if (!DriversKeys.isEmpty()) {
                                Log.d("Driver state", "The driver " + key + " is busy");
                                DriversRejected.add(key);
                                callADriver(DriversKeys.pop());

                            } else if (radius <= 20) {
                                Log.d("Driver state", "The driver " + key + " is busy and the  stack is empty");
                                DriversRejected.add(key);
                                radius++;
                                findDriversAround();
                            } else {
                                cancelRide();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        alertCanceledOrFinished();
                                    }
                                });
                            }
                        }
                    }).start();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }  else {
            Log.d("Thread state","is shutdown");
        }

    }

    private void listenToRideState(){
        if(requestBol){
             RideStateRef = FirebaseDatabase.getInstance().getReference()
                    .child("ride_info").child(currentRide.getId()).child("state");
             RideStateRefListener = RideStateRef.addValueEventListener(new ValueEventListener() {
                 @Override
                 public void onDataChange(@NonNull DataSnapshot snapshot) {
                     new Thread(new Runnable() {
                                 @Override
                                 public void run() {
                                     if(snapshot.exists() && requestBol){
                                         int state = snapshot.getValue(Integer.class);
                                         currentRide.setState(state);
                                         if(state == -1) {
                                             currentRide.setState(state);
                                             cancelRide();
                                             runOnUiThread(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     alertCanceledOrFinished();
                                                 }
                                             });
                                         }
                                         else if(state == 2){
                                             if(driverLocationRefListener != null)
                                                 driverLocationRef.removeEventListener(driverLocationRefListener);
                                             currentRide.setState(state);
                                             runOnUiThread(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     if(driverMarker != null)
                                                         driverMarker.remove();
                                                     if(rideMarker != null)
                                                         rideMarker.remove();
                                                     rideMarker = mMap.addMarker(new MarkerOptions()
                                                             .position(currentRide.getDestination().getCoordinates())
                                                             .title("Destination here"));
                                                 }
                                             });
                                         } else if (state == 3){
                                             cancelRide();

                                         }
                                     }
                                 }
                             }
                     ).start();
                 }

                 @Override
                 public void onCancelled(@NonNull DatabaseError error) {}
             });
        }  else {
            Log.d("Thread state","is shutdown");
        }
    }

    private void getDriverInfo(){
        if(requestBol){
            DatabaseReference driverInfoRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child("Driver").child(currentRide.getDriver().getId());
            driverInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(snapshot.exists() && requestBol){
                                String id = snapshot.getValue(String.class);
                                DriverObject driver = new DriverObject(id);
                                driver.parseData(snapshot);
                                currentRide.setDriver(driver);
                            } else if(!requestBol) {
                                Log.d("Request state", "request was canceled");
                            }
                        }
                    }).start();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }  else {
            Log.d("Thread state","is shutdown");
        }
    }

    private void checkDriverAccepted(String key){
        if(requestBol) {
            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                Log.d("InterruptedException","Interrupted");
            }
            DatabaseReference rideStateRef = FirebaseDatabase.getInstance().getReference()
                    .child("ride_info").child(currentRide.getId());
            rideStateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (snapshot.exists() && requestBol) {
                                int state = snapshot.child("state").getValue(Integer.class);
                                if (state == 1 && requestBol) {
                                    String driverId = snapshot.child("driverId").getValue(String.class);
                                    currentRide.setState(state);
                                    currentRide.setDriver(new DriverObject(driverId));
                                    Log.d("Driver state", "Driver accepted");
                                    Log.d("Request state", "ride state " + currentRide.getState());
                                    getDriverInfo();
                                    listenToRideState();
                                    getDriverLocation();
                                } else if(!requestBol) {
                                    Log.d("Request state", "request was canceled");
                                } else if (!DriversKeys.isEmpty()) {
                                    Log.d("Driver state", "Driver didn't accept");
                                    callADriver(DriversKeys.pop());
                                } else if (radius <= 20){
                                    Log.d("Driver state", "Driver didn't accept and stack is empty");
                                    DriversRejected.add(key);
                                    radius++;
                                    findDriversAround();
                                } else {
                                    cancelRide();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            alertCanceledOrFinished();
                                        }
                                    });
                                }
                            }
                        }
                    }).start();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else {
            Log.d("Thread state","is shutdown");
        }
    }

    private void alertCanceledOrFinished(){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet);

        Button ok =  dialog.findViewById(R.id.ok);
        TextView statement = dialog.findViewById(R.id.statement);
        if(currentRide.getState() == 3){
            statement.setText("The Ride was finished :)");
        } else if (currentRide.getState() == -1 || currentRide == null){
            statement.setText("The driver has canceled:(");
        } else {
            statement.setText("No driver was found :(");
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

    private double distance(LatLng latLng1, LatLng latLng2) {

        final int R = 6371;

        double latDistance = Math.toRadians(latLng2.latitude - latLng1.latitude);
        double lonDistance = Math.toRadians(latLng2.longitude - latLng1.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latLng1.latitude)) * Math.cos(Math.toRadians(latLng2.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2) + Math.pow(0.0, 2);

        return Math.sqrt(distance);
    }

}

