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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EmergencyListenerService extends Service {

    private static final String CHANNEL_ID = "EmergencyListenerChannel";
    private static final String TAG = "EmergencyService";

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e("GlobalCrash", "Unhandled exception in thread " + thread.getName(), e);
        });

        Log.d(TAG, "Service created and running...");
        createNotificationChannel();
        startForeground(1, createForegroundNotification("Listening for emergencies..."));

        listenForEmergencies();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "EmergencyListenerService stopped");
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

                DatabaseReference emergencyRef = refUser.child(userId).child("emergency");

                emergencyRef.addValueEventListener(new ValueEventListener() {
                    String lastLat = "";
                    String lastLng = "";

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) return;

                        String latStr = dataSnapshot.child("latitude").getValue(String.class);
                        String lngStr = dataSnapshot.child("longitude").getValue(String.class);

                        if (latStr != null && lngStr != null && (!latStr.equals(lastLat) || !lngStr.equals(lastLng))) {
                            lastLat = latStr;
                            lastLng = lngStr;

                            fetchEmergencyData(dataSnapshot, userId);
                        } else {
                            Log.d(TAG, "Emergency unchanged for user " + userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error observing emergency for user " + userId + ": " + error.getMessage());
                    }
                });
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch users: " + e.getMessage()));
    }


    private void fetchEmergencyData(DataSnapshot emergencySnapshot, String userId) {
        try {
            String latStr = emergencySnapshot.child("latitude").getValue(String.class);
            String lngStr = emergencySnapshot.child("longitude").getValue(String.class);
            String serviceId = emergencySnapshot.child("serviceId").getValue(String.class);

            if (latStr != null && lngStr != null && serviceId != null) {
                try {
                    double lat = Double.parseDouble(latStr);
                    double lng = Double.parseDouble(lngStr);
                    Log.d(TAG, "Emergency by user " + userId + " -> Lat: " + lat + ", Lng: " + lng + ", Service ID: " + serviceId);
                    findNearestVolunteers(lat, lng, serviceId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid latitude/longitude format: " + latStr + ", " + lngStr, e);
                }
            } else {
                Log.w(TAG, "Emergency data incomplete or invalid.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while processing emergency data: " + e.getMessage(), e);
        }
    }

    public void findNearestVolunteers(double userLat, double userLng, String serviceId) {
        searchForVolunteers(userLat, userLng, 2000, serviceId);
    }

    private void searchForVolunteers(double userLat, double userLng, int radiusMeters, String emergencyServiceId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    Log.d("DebugFirebase", "Step 1 Com");
                    List<String> nearbyVolunteers = new ArrayList<>();

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        Boolean isVolunteer = userSnapshot.child("isVolunteer").getValue(Boolean.class);
                        Boolean isAvailable = userSnapshot.child("isAvailable").getValue(Boolean.class);
                        String volunteerServiceId = userSnapshot.child("serviceId").getValue(String.class);

                        if (Boolean.TRUE.equals(isVolunteer) && Boolean.TRUE.equals(isAvailable) &&
                                volunteerServiceId != null && volunteerServiceId.equals(emergencyServiceId)) {

                            Double latNum = userSnapshot.child("locations").child("latitude").getValue(Double.class);
                            Double lngNum = userSnapshot.child("locations").child("longitude").getValue(Double.class);

                            if (latNum != null && lngNum != null) {
                                float[] results = new float[1];
                                Location.distanceBetween(userLat, userLng, latNum, lngNum, results);

                                if (results[0] <= radiusMeters) {
                                    Log.d(TAG, "Volunteer found within " + results[0] + "m: " + userSnapshot.getKey());
                                    nearbyVolunteers.add(userSnapshot.getKey());
                                }
                            }
                        }
                    }

                    if (!nearbyVolunteers.isEmpty()) {
                        Log.d(TAG, "Volunteers found: " + nearbyVolunteers.size());
                        sendNotificationsToVolunteers(nearbyVolunteers);
                    } else if (radiusMeters < 10000) {
                        Log.d(TAG, "No volunteers found within " + radiusMeters + "m. Expanding search...");
                        new Handler(getMainLooper()).postDelayed(() ->
                                        searchForVolunteers(userLat, userLng, radiusMeters + 3000, emergencyServiceId),
                                10000);
                    } else {
                        Log.d(TAG, "Stop - No volunteer found nearby " + nearbyVolunteers.size());
                                Toast.makeText(getApplicationContext(), "No volunteer found nearby.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Volunteer search failed: " + e.getMessage(), e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase read error: ", error.toException());
            }
        });
    }

    private void sendNotificationsToVolunteers(List<String> volunteerIds) {
        String currentUserId = GlobalData.getUserId(EmergencyListenerService.this);
        for (String userId : volunteerIds) {
            if (currentUserId != null){
                if (userId.equals(currentUserId)) {
                    showEmergencyNotification("Emergency Alert", "Someone nearby needs help! " + userId);
                }
            }else {
                Log.w(TAG, "Current user ID is null. Skipping self-notification.");
            }
            Log.d(TAG, "Sending notification to volunteer ID: " + userId);
            // TODO: Add FCM push notification logic here
        }
    }

    private void showEmergencyNotification(String title, String message) {
        Log.d(TAG, "Displaying notification: " + title + " - " + message);

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fromNotification", true);  // Optional: pass extra data

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
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
        } else {
            Log.e(TAG, "NotificationManager is null. Notification not shown.");
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
            } else {
                Log.e(TAG, "NotificationManager is null. Channel not created.");
            }
        }
    }
}
