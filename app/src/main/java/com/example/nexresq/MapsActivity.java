package com.example.nexresq;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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
import java.util.List;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        distanceTextView = findViewById(R.id.distanceTextView);
        addressTextView = findViewById(R.id.addressTextView);

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
}