package com.example.nexresq;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private static final String CHANNEL_ID = "foreground_service_channel";
    private LocationRequest locationRequest;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;
    private static final float MIN_DISTANCE_THRESHOLD = 10.0f; // meters


    // Write a message to the database
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("user1");

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(); // ✅ Create notification channel
        initLocationRequest(); // ✅ Initialize location request
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ✅ Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Start location updates
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        }

        // Create the notification
        Intent notificationIntent = new Intent(this, MainActivity.class); // Redirect to MainActivity on click
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⏰ Emergency Ringtone Active")
                .setContentText("Tap to open the emergency panel")
                .setSmallIcon(R.drawable.fire_icon)
                .setContentIntent(pendingIntent) // Add click action
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        // Start the service in the foreground
        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // Initialize the location request (better practice to do this in a method)
    private void initLocationRequest() {
        locationRequest = new LocationRequest.Builder(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                10_000L // still required as a fallback interval
        )
                .setMinUpdateDistanceMeters(10f) // 🔥 Trigger update when moved 10 meters
                .setMinUpdateIntervalMillis(5_000L) // Minimum time between updates
                .build();
    }

    // LocationCallback to handle location updates
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;

            for (Location location : locationResult.getLocations()) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                float[] results = new float[1];
                Location.distanceBetween(lastLatitude, lastLongitude, lat, lng, results);
                float distanceInMeters = results[0];

                if (distanceInMeters >= MIN_DISTANCE_THRESHOLD) {
                    lastLatitude = lat;
                    lastLongitude = lng;

                    Map<String, Object> locationMap = new HashMap<>();
                    locationMap.put("latitude", lat);
                    locationMap.put("longitude", lng);

//                    myRef.setValue(locationMap);
                    Log.d("MY LOC", "Updated to Firebase - Latitude: " + lat + ", Longitude: " + lng + ", dis: " + distanceInMeters);

                } else {
                    Log.d("MY LOC", "Location changed less than threshold: " + distanceInMeters + "m, not updating Firebase.");
                }
            }
        }
    };

    // Create the notification channel (for API >= 26)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Ringtone Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
