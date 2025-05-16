package com.example.nexresq;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class GlobalData {

        public static String BASE_URL = "https://coachingwood.in/nexresq/";
        public static final String PREFS_NAME = "MyPrefs";

        // SharedPreferences helper methods
        public static String getUserId(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("userId", null);
        }

        public static String getFirstName(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("firstName", "no user");
        }

        public static String getLastName(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("lastName", "no user");
        }

        public static String getOrganizationId(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("organizationId", "no user");
        }

        public static String isOrganization(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("isOrganization", "0");
        }

        public static String isVolunteer(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("isVolunteer", "0");
        }

        // ✅ 1. Get current location with updates
        public static void getCurrentLocation(Context context, MyLocationListener listener) {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(5000);
                locationRequest.setFastestInterval(2000);

                LocationCallback locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                                if (locationResult == null) return;
                                Location location = locationResult.getLastLocation();
                                if (location != null) {
                                        listener.onLocationReceived(location);
                                        fusedLocationClient.removeLocationUpdates(this); // one-time
                                }
                        }
                };

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                        return;
                }

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

        // ✅ 2. Get last known location (one-time, instant)
        public static void getLastKnownLocation(Context context, MyLocationListener listener) {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                        return;
                }

                fusedLocationClient.getLastLocation().addOnSuccessListener((Activity) context, location -> {
                        if (location != null) {
                                listener.onLocationReceived(location);
                        } else {
                                listener.onLocationReceived(null); // no cached location available
                        }
                });
        }

        // Custom callback interface
        public interface MyLocationListener {
                void onLocationReceived(Location location);
        }
}
