package com.example.nexresq;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

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

        // Custom callback interface for location
        public interface MyLocationListener {
                void onLocationReceived(Location location);
        }

        // Custom callback interface for AccessToken
        public interface AccessTokenCallback {
                void onTokenReceived(String token);
                void onError(String error);
        }

        public static void getAccessTokenFromUrl(Context context, String serviceAccountUrl, AccessTokenCallback callback) {
                Log.d("AccessToken", "Starting token retrieval from: " + serviceAccountUrl);

                VolleyHelper.sendGetRequest(context, serviceAccountUrl, new VolleyHelper.VolleyCallback() {
                        @Override
                        public void onSuccess(String response) {
                                Log.d("AccessToken", "Service account JSON downloaded successfully");

                                new Thread(() -> {
                                        try {
                                                Log.d("AccessToken", "Converting response to InputStream");
                                                ByteArrayInputStream inputStream = new ByteArrayInputStream(response.getBytes());

                                                Log.d("AccessToken", "Creating GoogleCredentials from InputStream");
                                                GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
                                                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));

                                                Log.d("AccessToken", "Refreshing credentials if expired");
                                                googleCredentials.refreshIfExpired();  // ✅ Now runs on background thread

                                                AccessToken token = googleCredentials.getAccessToken();

                                                Log.d("AccessToken", "Access token obtained successfully: " + token.getTokenValue());

                                                // Send result to main thread
                                                new Handler(Looper.getMainLooper()).post(() -> callback.onTokenReceived(token.getTokenValue()));

                                        } catch (IOException e) {
                                                Log.e("AccessToken", "Failed to parse credentials or get token", e);
                                                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Token error: " + e.getMessage()));
                                        }
                                }).start();
                        }

                        @Override
                        public void onError(String error) {
                                Log.e("AccessToken", "Failed to download service account JSON: " + error);
                                callback.onError("Failed to download service account JSON: " + error);
                        }
                });
        }
}
