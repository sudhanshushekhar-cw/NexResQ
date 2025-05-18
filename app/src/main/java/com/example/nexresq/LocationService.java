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
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "foreground_service_channel";
    private static final float MIN_DISTANCE_THRESHOLD = 10.0f; // meters

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;
    private DatabaseReference ref;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initLocationRequest();

        String userId = GlobalData.getUserId(getApplicationContext());
        if (userId == null || userId.isEmpty()) {
            Log.e("LocationService", "❌ User ID is null or empty. Stopping service.");
            stopSelf();
            return;
        }

        ref = FirebaseDatabase.getInstance().getReference("user")
                .child(String.valueOf(userId))
                .child("locations");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "❌ Location permission not granted. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⏰ Location Services")
                .setContentText("This services required to get alert")
                .setSmallIcon(R.drawable.fire_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();

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

    private void initLocationRequest() {
        locationRequest = new LocationRequest.Builder(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                10_000L // fallback interval
        )
                .setMinUpdateDistanceMeters(10f)
                .setMinUpdateIntervalMillis(5_000L)
                .build();
    }

    private final LocationCallback locationCallback = new LocationCallback() {
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
                    locationMap.put("last_updated", System.currentTimeMillis());
                    locationMap.put("longitude", lng);
                    locationMap.put("latitude", lat);

//                    ref.setValue(locationMap);
                    Log.d("MY LOC", "✅ Firebase Updated - Lat: " + lat + ", Lng: " + lng + ", Distance: " + distanceInMeters);
                } else {
                    Log.d("MY LOC", "⏩ Skipped update. Distance moved: " + distanceInMeters + "m");
                }
            }
        }
    };

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
