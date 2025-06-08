package com.example.nexresq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.nexresq.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private DatabaseReference ref, ref2;
    private LatLng emergencyLocation;
    private Marker volunteerMarker;
    private Polyline currentRoute;
    private static final String TAG = "MapsActivity";

    private String DIRECTIONS_API_KEY;
    private final Gson gson = new Gson();

    private TextView distanceTextView;
    private TextView addressTextView;
    private Switch switchButton;
    private Button resqDoneButton;

    private ValueEventListener geofenceListener;
    private final float GEOFENCE_RADIUS_METERS = 4000f; // 4 km
    String volunteerId;
    String AccessToken;
    String serviceAccountPath = GlobalData.BASE_URL + "nexresq-6bc701ab1ee6.json";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        distanceTextView = findViewById(R.id.distanceTextView);
        addressTextView = findViewById(R.id.addressTextView);
        switchButton = findViewById(R.id.switchButton);
        resqDoneButton = findViewById(R.id.resqDoneButton);


        Log.d(TAG, "onCreate started");

        // ✅ Load API key
        DIRECTIONS_API_KEY = getString(R.string.google_maps_key_browser);
        Log.d(TAG, "API Key loaded: " + (DIRECTIONS_API_KEY != null ? "Success" : "Failed"));
        // Custom icons
        Bitmap carIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ambulance);
        Bitmap emergencyIcon = BitmapFactory.decodeResource(getResources(), R.drawable.sos_location);

        // ✅ Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // ✅ Load Emergency Location
        Intent intent = getIntent();
        String userIdEme = intent.getStringExtra("userIdEme");
        boolean isGeofencingFeature = intent.getBooleanExtra("isGeofencingFeature", true);
        AccessToken = "null";
//        String userIdEme = "22";
//        boolean isGeofencingFeature = true;
        volunteerId = GlobalData.getUserId(MapsActivity.this);
        //hide geofencing feature for user (requested emergency)

        // --- START: MODIFIED RESQ DONE BUTTON CLICK LISTENER ---
        resqDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a confirmation dialog
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("Confirm Rescue Completion")
                        .setMessage("Are you sure you want to mark this rescue as complete? This action cannot be undone.")
                        .setPositiveButton("Yes, Complete", (dialog, which) -> {
                            // User confirmed, proceed with completing the rescue
                            DatabaseReference userRef = FirebaseDatabase.getInstance()
                                    .getReference("user")
                                    .child(GlobalData.getUserId(MapsActivity.this));

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("isAvailable", true); // Make the volunteer available again
                            userData.put("emergencyStatus", "Completed"); // Update emergency status
                            userRef.updateChildren(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Volunteer status updated to available and emergencyStatus to Completed.");

                                        // Now remove the emergency node for the emergency user
                                        DatabaseReference emergencyUserRef = FirebaseDatabase.getInstance()
                                                .getReference("user")
                                                .child(userIdEme); // Use the userIdEme from the intent
                                        emergencyUserRef.child("emergency").removeValue()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    finish();
                                                    Log.d(TAG, "Emergency node removed for user: " + userIdEme);
                                                    // Optionally, navigate back or show a success message
                                                    // For example, if you want to go back to a previous activity:
                                                    // finish();
                                                    // Or show a toast:
                                                    // Toast.makeText(MapsActivity.this, "Rescue completed successfully!", Toast.LENGTH_LONG).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to remove emergency node for user " + userIdEme + ": " + e.getMessage());
                                                    // Handle error (e.g., show a toast)
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to update volunteer status: " + e.getMessage());
                                        // Handle error (e.g., show a toast)
                                    });
                        })
                        .setNegativeButton("No, Cancel", (dialog, which) -> {
                            // User cancelled, do nothing
                            dialog.dismiss();
                            Log.d(TAG, "Rescue completion cancelled by user.");
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert) // Optional: Add an alert icon
                        .show();
            }
        });

        GlobalData.getAccessTokenFromUrl(MapsActivity.this, serviceAccountPath, new GlobalData.AccessTokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                // Use the access token here
                Log.d("AccessToken - Success", token);
                AccessToken = token;
            }

            @Override
            public void onError(String error) {
                // Handle the error
                Log.e("AccessTokenError", error);
            }
        });

        if (!isGeofencingFeature){
            switchButton.setVisibility(View.GONE);
            resqDoneButton.setVisibility(View.GONE);
        }

        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.d(TAG, "Switch ON - Start sending notifications to nearby users");

                // Call your geofencing/notification logic here
                startSendingGeofenceNotifications();
            } else {
                Log.d(TAG, "Switch OFF - Stop sending notifications");

                // Optional: Stop any active notifications or location triggers
                stopSendingGeofenceNotifications();
            }
        });


        ref = FirebaseDatabase.getInstance().getReference("user").child(userIdEme);

        Log.d(TAG, "Fetching emergency location for userId: " + userIdEme);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get location from 'locations' node
                DataSnapshot emergencySnapshot = snapshot.child("emergency");
                Double lat = Double.valueOf(emergencySnapshot.child("latitude").getValue(String.class));
                Double lng = Double.valueOf(emergencySnapshot.child("longitude").getValue(String.class));

                if (lat != null && lng != null) {
                    emergencyLocation = new LatLng(lat, lng);
                    Log.d(TAG, "Emergency location: " + emergencyLocation);

                    if (mMap != null) {
                        mMap.addMarker(new MarkerOptions()
                                .position(emergencyLocation)
                                .title("Emergency Location")
                                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(emergencyIcon, 100, 100, false))));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(emergencyLocation, 15));
                        Log.d(TAG, "Emergency marker placed");

                        String apiKey = getString(R.string.google_maps_key);
                        String GOOGLE_GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&key=" + apiKey;

                        VolleyHelper.sendGetRequest(MapsActivity.this, GOOGLE_GEOCODING_URL, new VolleyHelper.VolleyCallback() {
                            @Override
                            public void onSuccess(String response) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    JSONArray results = jsonObject.getJSONArray("results");
                                    if (results.length() > 0) {
                                        String address = results.getJSONObject(0).getString("formatted_address");
                                        addressTextView.setText(address);
                                    } else {
                                        Log.d("Geocoding", "No address found");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e("GeocodingError", "Parsing error: " + e.getMessage());
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("VolleyError", error);
                            }
                        });

                    } else {
                        Log.w(TAG, "Map not ready yet when setting emergency marker");
                    }
                } else {
                    Log.e(TAG, "Emergency location is null");
                }

                // ✅ Get volunteer ID from 'emergency' node
                String volunteerId = emergencySnapshot.child("volunteerId").getValue(String.class);

                if (volunteerId != null) {
                    ref2 = FirebaseDatabase.getInstance().getReference("user").child(volunteerId).child("locations");
                    trackVolunteerLive(carIcon);
                } else {
                    Log.e(TAG, "Volunteer ID is null");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error getting emergency location: " + error.getMessage());
            }
        });

    }

    private void trackVolunteerLive(Bitmap carIcon) {
        Log.d(TAG, "Starting to track volunteer live...");

        ref2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null) {
                    Log.e(TAG, "Map not ready when tracking volunteer");
                    return;
                }

                Double vLat = snapshot.child("latitude").getValue(Double.class);
                Double vLng = snapshot.child("longitude").getValue(Double.class);

                if (vLat != null && vLng != null) {
                    LatLng volunteerLocation = new LatLng(vLat, vLng);
                    Log.d(TAG, "Volunteer location: " + volunteerLocation);

                    if (volunteerMarker == null) {
                        volunteerMarker = mMap.addMarker(new MarkerOptions()
                                .position(volunteerLocation)
                                .title("Volunteer")
                                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(carIcon, 100, 100, false))));
                        Log.d(TAG, "Volunteer marker added");
                    } else {
                        volunteerMarker.setPosition(volunteerLocation);
                        Log.d(TAG, "Volunteer marker moved");
                    }

                    // Calculate distance if emergency location is available
                    if (emergencyLocation != null) {
                        android.location.Location emergencyLoc = new android.location.Location("emergency");
                        emergencyLoc.setLatitude(emergencyLocation.latitude);
                        emergencyLoc.setLongitude(emergencyLocation.longitude);

                        android.location.Location volunteerLoc = new android.location.Location("volunteer");
                        volunteerLoc.setLatitude(volunteerLocation.latitude);
                        volunteerLoc.setLongitude(volunteerLocation.longitude);

                        float distanceInMeters = emergencyLoc.distanceTo(volunteerLoc); // returns meters
                        distanceTextView.setText("Distance: " + String.format("%.2f", distanceInMeters / 1000) + " km");

                        // Adjust camera
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(emergencyLocation);
                        builder.include(volunteerLocation);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                        Log.d(TAG, "Camera adjusted to fit both markers");
                    }

                    drawRoute(volunteerLocation, emergencyLocation);
                }
                else {
                    Log.e(TAG, "Volunteer location is null");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error tracking volunteer: " + error.getMessage());
            }
        });
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        if (origin == null || destination == null) {
            Log.e(TAG, "drawRoute skipped - null origin or destination");
            return;
        }

        Log.d(TAG, "Drawing route from " + origin + " to " + destination);

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=driving" +
                "&key=" + DIRECTIONS_API_KEY;

        VolleyHelper.sendGetRequest(this, url, new VolleyHelper.VolleyCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Directions API success");

                JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
                JsonArray routes = jsonObject.getAsJsonArray("routes");

                if (routes == null || routes.size() == 0) {
                    Log.e(TAG, "No routes found");
                    return;
                }

                JsonObject route = routes.get(0).getAsJsonObject();
                JsonObject overviewPolyline = route.getAsJsonObject("overview_polyline");
                String encodedPoints = overviewPolyline.get("points").getAsString();

                List<LatLng> routePoints = decodePolyline(encodedPoints);
                Log.d(TAG, "Route decoded with " + routePoints.size() + " points");

                runOnUiThread(() -> {
                    if (currentRoute != null) currentRoute.remove();

                    currentRoute = mMap.addPolyline(new PolylineOptions()
                            .addAll(routePoints)
                            .width(12)
                            .color(Color.BLUE)
                            .geodesic(true));
                    Log.d(TAG, "Route drawn on map");
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Directions API error: " + error);
            }
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        Log.d(TAG, "Decoding polyline...");
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length(), lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }

        Log.d(TAG, "Polyline decoding complete");
        return poly;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready");
    }


    //GEO Fencing code
    private void startSendingGeofenceNotifications() {
        String volunteerId = GlobalData.getUserId(this);

        DatabaseReference volunteerRef = FirebaseDatabase.getInstance()
                .getReference("user")
                .child(volunteerId)
                .child("locations");

        geofenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double vLat = snapshot.child("latitude").getValue(Double.class);
                Double vLng = snapshot.child("longitude").getValue(Double.class);

                if (vLat == null || vLng == null) {
                    Log.e(TAG, "Volunteer location missing.");
                    return;
                }

                Location volunteerLoc = new Location("volunteer");
                volunteerLoc.setLatitude(vLat);
                volunteerLoc.setLongitude(vLng);

                DatabaseReference allUsersRef = FirebaseDatabase.getInstance().getReference("user");

                allUsersRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DataSnapshot userSnap : task.getResult().getChildren()) {
                            String uid = userSnap.getKey();
                            if (uid.equals(volunteerId)) continue; // Skip self

                            DataSnapshot locationSnap = userSnap.child("locations");
                            Double uLat = locationSnap.child("latitude").getValue(Double.class);
                            Double uLng = locationSnap.child("longitude").getValue(Double.class);

                            if (uLat == null || uLng == null) {
                                Log.w(TAG, "User " + uid + " has no location data.");
                                continue;
                            }

                            Location userLoc = new Location("user");
                            userLoc.setLatitude(uLat);
                            userLoc.setLongitude(uLng);

                            float distance = volunteerLoc.distanceTo(userLoc);

                            Log.d(TAG, "Checking user " + uid + " - distance: " + distance);

                            if (distance <= GEOFENCE_RADIUS_METERS) {
                                Log.d(TAG, "✅ User " + uid + " is within 4km. Sending alert...");
                                sendPushNotificationToUser(uid);
                            } else {
                                Log.d(TAG, "❌ User " + uid + " is OUTSIDE 4km. Skipping alert.");
                            }
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Geofencing cancelled: " + error.getMessage());
            }
        };

        // Start listening for volunteer movement
        volunteerRef.addValueEventListener(geofenceListener);
    }

    private void stopSendingGeofenceNotifications() {
        String volunteerId = GlobalData.getUserId(this);
        DatabaseReference volunteerRef = FirebaseDatabase.getInstance()
                .getReference("user")
                .child(volunteerId)
                .child("locations");

        if (geofenceListener != null) {
            volunteerRef.removeEventListener(geofenceListener);
            Log.d(TAG, "Stopped sending geofencing notifications.");
        }
    }

    private void sendPushNotificationToUser(String uid) {
        DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("user")
                .child(uid)
                .child("fcmTokens");

        tokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userFcmToken = snapshot.getValue(String.class);

                if (userFcmToken != null && !userFcmToken.isEmpty()) {
                    sendFCMMessage(AccessToken, userFcmToken, "🚨 URGENT: AMBULANCE APPROACHING (4 KM Behind)",  "Ambulance 🚑 is approaching your area. Clear the route IMMEDIATELY!","nexresq");

                    Log.d(TAG, "Sending push notification to user " + uid + "/" + userFcmToken);
                } else {
                    Log.w(TAG, "❌ No FCM token found for user " + uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error while reading token for user " + uid + ": " + error.getMessage());
            }
        });
    }


    public void sendFCMMessage(String accessToken, String userFcmToken,
                               String title, String body, String firebaseProjectId) {

        String url = "https://fcm.googleapis.com/v1/projects/" + firebaseProjectId + "/messages:send";

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("Content-Type", "application/json");

        try {
            JSONObject payload = new JSONObject();
            JSONObject message = new JSONObject();
            JSONObject notification = new JSONObject();
            JSONObject androidNotification = new JSONObject();
            JSONObject android = new JSONObject();

            notification.put("title", title);
            notification.put("body", body);

            androidNotification.put("click_action", "FLUTTER_NOTIFICATION_CLICK");
            androidNotification.put("priority", "HIGH");

            message.put("token", userFcmToken);
            message.put("notification", notification);
            message.put("android", android);

            payload.put("message", message);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                    response -> Log.d("FCM", "✅ Notification sent: " + response.toString()),
                    error -> Log.e("FCM", "❌ Notification failed: " + error.toString())) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(MapsActivity.this);
            queue.add(request);

        } catch (Exception e) {
            Log.e("FCM", "JSON exception: " + e.getMessage());
        }
    }
}