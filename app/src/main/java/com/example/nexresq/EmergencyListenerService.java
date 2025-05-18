package com.example.nexresq;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyListenerService extends Service {

    private static final String CHANNEL_ID = "EmergencyListenerChannel";
    private static final String TAG = "EmergencyService";

    private String lastLat = "";
    private String lastLng = "";
    private String lastEmergencyUserId = "";
    private String lastServiceId = "";
    private String emergencyId = "";

    private final Map<String, Boolean> firstLoadMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e("GlobalCrash", "Unhandled exception in thread " + thread.getName(), e);
        });

        Log.d(TAG, "[TEST] Service created and running...");
        createNotificationChannel();
        startForeground(1, createForegroundNotification("Listening for emergencies..."));

        listenForEmergencies();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "[TEST] onStartCommand received");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "[TEST] EmergencyListenerService stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void listenForEmergencies() {
        DatabaseReference refUser = FirebaseDatabase.getInstance().getReference("user");

        refUser.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                String userId = userSnapshot.getKey();
                if (userId == null) continue;

                Log.d(TAG, "[TEST] Listening for emergency updates from user: " + userId);

                DatabaseReference emergencyRef = refUser.child(userId).child("emergency");
                firstLoadMap.put(userId, true);

                emergencyRef.addValueEventListener(new ValueEventListener() {
                    String prevLat = "";
                    String prevLng = "";

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) return;

                        String latStr = dataSnapshot.child("latitude").getValue(String.class);
                        String lngStr = dataSnapshot.child("longitude").getValue(String.class);
                        String emeId = dataSnapshot.child("emergencyId").getValue(String.class);

                        Log.d(TAG, "[TEST] Data change detected for user: " + userId + " -> Lat: " + latStr + ", Lng: " + lngStr);

                        if (firstLoadMap.getOrDefault(userId, true)) {
                            firstLoadMap.put(userId, false);
                            Log.d(TAG, "[TEST] Initial load skipped for user " + userId);
                            return;
                        }

                        if (latStr != null && lngStr != null &&
                                (!latStr.equals(prevLat) || !lngStr.equals(prevLng))) {

                            prevLat = latStr;
                            prevLng = lngStr;
                            lastLat = latStr;
                            lastLng = lngStr;
                            lastEmergencyUserId = userId;
                            emergencyId = emeId;

                            fetchEmergencyData(dataSnapshot, userId);
                        } else {
                            Log.d(TAG, "[TEST] No location change for user " + userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "[TEST] Error observing emergency for user " + userId + ": " + error.getMessage());
                    }
                });
            }
        }).addOnFailureListener(e -> Log.e(TAG, "[] FaTESTiled to fetch users: " + e.getMessage()));
    }

    private void fetchEmergencyData(DataSnapshot emergencySnapshot, String userId) {
        try {
            String latStr = emergencySnapshot.child("latitude").getValue(String.class);
            String lngStr = emergencySnapshot.child("longitude").getValue(String.class);
            String serviceId = emergencySnapshot.child("serviceId").getValue(String.class);

            Log.d(TAG, "[TEST] Processing emergency data for userId: " + userId);

            if (latStr != null && lngStr != null && serviceId != null) {
                double lat = Double.parseDouble(latStr);
                double lng = Double.parseDouble(lngStr);

                lastServiceId = serviceId;

                Log.d(TAG, "[TEST] Emergency details -> Lat: " + lat + ", Lng: " + lng + ", Service ID: " + serviceId);
                findNearestVolunteers(lat, lng, serviceId);
            } else {
                Log.w(TAG, "[TEST] Emergency data incomplete or invalid.");
            }
        } catch (Exception e) {
            Log.e(TAG, "[TEST] Error while processing emergency data: " + e.getMessage(), e);
        }
    }

    public void findNearestVolunteers(double userLat, double userLng, String serviceId) {
        Log.d(TAG, "[TEST] Finding volunteers near Lat: " + userLat + ", Lng: " + userLng);
        searchForVolunteers(userLat, userLng, 2000, serviceId);
    }

    private void searchForVolunteers(double userLat, double userLng, int radiusMeters, String emergencyServiceId) {
        Log.d(TAG, "[TEST] Searching for volunteers within " + radiusMeters + " meters for serviceId: " + emergencyServiceId);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<String> nearbyVolunteers = new ArrayList<>();

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String uid = userSnapshot.getKey();
                        Boolean isVolunteer = userSnapshot.child("isVolunteer").getValue(Boolean.class);
                        Boolean isAvailable = userSnapshot.child("isAvailable").getValue(Boolean.class);
                        String volunteerServiceId = userSnapshot.child("serviceId").getValue(String.class);

                        Log.d(TAG, "[TEST] Checking user: " + uid + ", isVolunteer=" + isVolunteer + ", isAvailable=" + isAvailable + ", serviceId=" + volunteerServiceId);

                        if (Boolean.TRUE.equals(isVolunteer) && Boolean.TRUE.equals(isAvailable) &&
                                emergencyServiceId.equals(volunteerServiceId)) {

                            Double lat = userSnapshot.child("locations").child("latitude").getValue(Double.class);
                            Double lng = userSnapshot.child("locations").child("longitude").getValue(Double.class);

                            if (lat != null && lng != null) {
                                float[] results = new float[1];
                                Location.distanceBetween(userLat, userLng, lat, lng, results);

                                Log.d(TAG, "[TEST] Distance to " + uid + ": " + results[0]);

                                if (results[0] <= radiusMeters) {
                                    nearbyVolunteers.add(uid);
                                }
                            }
                        }
                    }

                    if (!nearbyVolunteers.isEmpty()) {
                        Log.d(TAG, "[TEST] Nearby volunteers: " + nearbyVolunteers);
                        sendNotificationsToVolunteers(nearbyVolunteers, userLat, userLng, radiusMeters, emergencyServiceId);
                    } else if (radiusMeters < 10000) {
                        Log.d(TAG, "[TEST] No nearby volunteers found. Expanding search radius.");
                        new Handler(getMainLooper()).postDelayed(() ->
                                searchForVolunteers(userLat, userLng, radiusMeters + 3000, emergencyServiceId), 10000);
                    } else {
                        Log.d(TAG, "[TEST] No volunteers found even after expanding radius.");
                        Toast.makeText(getApplicationContext(), "No volunteer found nearby.", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "[TEST] Volunteer search failed: " + e.getMessage(), e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "[TEST] Firebase error: ", error.toException());
            }
        });
    }

    private void sendNotificationsToVolunteers(List<String> volunteerIds, double userLat, double userLng,
                                               int radiusMeters, String serviceId) {
        String currentUserId = GlobalData.getUserId(EmergencyListenerService.this);

        for (String userIdVol : volunteerIds) {
            Log.d(TAG, "[TEST] Notifying volunteer: " + userIdVol);

            if (currentUserId != null && userIdVol.equals(currentUserId)) {
                Log.d(TAG, "[TEST] Current device user is volunteer, sending notification");

                showEmergencyNotification("Emergency Alert", "Someone nearby needs help!",
                        lastEmergencyUserId, lastLat, lastLng, lastServiceId, emergencyId);

                new Handler(getMainLooper()).postDelayed(() -> {
                    checkIfEmergencyAccepted(currentUserId, (isAccepted) -> {
                        Log.d(TAG, "[TEST] Emergency accepted by current user? " + isAccepted);

                        if (!isAccepted && radiusMeters < 10000) {
                            searchForVolunteers(userLat, userLng, radiusMeters + 3000, serviceId);
                        } else if (!isAccepted) {
                            Log.d(TAG, "[TEST] No volunteers accepted the emergency.");
                        }
                    });
                }, 20000);
            }
        }
    }

    private void checkIfEmergencyAccepted(String userId, EmergencyAcceptanceCallback callback) {
        Log.d(TAG, "[TEST] Checking emergencyAccepted status for user: " + userId);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user").child(userId).child("emergencyAccepted");

        ref.get().addOnSuccessListener(snapshot -> {
            Boolean isAccepted = snapshot.getValue(Boolean.class);
            callback.onResult(Boolean.TRUE.equals(isAccepted));
        }).addOnFailureListener(e -> {
            Log.e(TAG, "[TEST] Failed to check emergency accepted: " + e.getMessage());
            callback.onResult(false);
        });
    }

    interface EmergencyAcceptanceCallback {
        void onResult(boolean isAccepted);
    }

    private void showEmergencyNotification(String title, String message, String userId,
                                           String latitude, String longitude, String serviceId, String emergencyId) {

        Log.d(TAG, "[TEST] Showing emergency notification to user");

        Intent intent = new Intent(this, EmergencyResponse.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fromNotification", true);
        intent.putExtra("userId", userId);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("serviceId", serviceId);
        intent.putExtra("emergencyId", emergencyId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    private Notification createForegroundNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Emergency Listener Running")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Emergency Channel";
            String description = "Listening for emergency changes";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

}
