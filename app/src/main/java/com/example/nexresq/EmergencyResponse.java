package com.example.nexresq;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyResponse extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean isMapReady = false;
    private boolean isLocationReady = false;

    private double latEme;
    private double lonEme;
    private String userIdEme;
    private String emergencyId;

    private TextView distanceTextView;
    private LatLng currentLatLng;
    private LatLng emergencyLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_response);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        userIdEme = intent.getStringExtra("userId");
        emergencyId = intent.getStringExtra("emergencyId");
        latEme = Double.parseDouble(intent.getStringExtra("latitude"));
        lonEme = Double.parseDouble(intent.getStringExtra("longitude"));
        emergencyLatLng = new LatLng(latEme, lonEme);

        TextView addressTextView = findViewById(R.id.addressTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        Button acceptButton = findViewById(R.id.acceptButton);
        addressTextView.setText("Loading....");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("user")
                .child(userIdEme)
                .child("emergency");

        String apiKey = getString(R.string.google_maps_key);
        String GOOGLE_GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latEme + "," + lonEme + "&key=" + apiKey;

        VolleyHelper.sendGetRequest(EmergencyResponse.this, GOOGLE_GEOCODING_URL, new VolleyHelper.VolleyCallback() {
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


        String POST_URL = GlobalData.BASE_URL + "emergency/update_emergency_status.php";
        Map<String, String> postParams = new HashMap<>();
        postParams.put("emergencyId", emergencyId);
        postParams.put("volunteerId", GlobalData.getUserId(EmergencyResponse.this));

        acceptButton.setOnClickListener(v -> {
            Map<String, Object> update = new HashMap<>();
            update.put("status", "Accepted");
            VolleyHelper.sendPostRequest(EmergencyResponse.this, POST_URL, postParams, new VolleyHelper.VolleyCallback() {
                @Override
                public void onSuccess(String response) {
                    ref.updateChildren(update)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(EmergencyResponse.this, "Status updated", Toast.LENGTH_SHORT).show();
                                fetchRouteAndDrawWithVolley();  // <-- Call it here!
                            })
                            .addOnFailureListener(e -> Toast.makeText(EmergencyResponse.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
                @Override
                public void onError(String error) {
                    Log.e("VolleyPostError", "Error: " + error); // Add this line
                    Toast.makeText(EmergencyResponse.this, "Some Error Occurred", Toast.LENGTH_SHORT).show();
                }
            });

        });

        GlobalData.getLastKnownLocation(this, location -> {
            if (location != null) {
                double currentLat = location.getLatitude();
                double currentLon = location.getLongitude();
                currentLatLng = new LatLng(currentLat, currentLon);
                isLocationReady = true;

                float[] results = new float[1];
                Location.distanceBetween(currentLat, currentLon, latEme, lonEme, results);
                float distanceInKm = results[0] / 1000;
                distanceTextView.setText(String.format("%.2f km", distanceInKm));

                drawMarkersAndLine();
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        isMapReady = true;

        // Enable default blue dot
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        drawMarkersAndLine();
    }

    private void drawMarkersAndLine() {
        if (!isMapReady || !isLocationReady) return;

        mMap.clear();

        // Custom icons
        Bitmap carIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ambulance);
        Bitmap emergencyIcon = BitmapFactory.decodeResource(getResources(), R.drawable.sos_location);

        // Add emergency marker
        mMap.addMarker(new MarkerOptions()
                .position(emergencyLatLng)
                .title("Emergency")
                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(emergencyIcon, 100, 100, false))));

        // Add user location marker
        mMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title("You")
                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(carIcon, 100, 100, false))));

        // Line between user and emergency
        mMap.addPolyline(new PolylineOptions()
                .add(currentLatLng, emergencyLatLng)
                .width(8)
                .color(0xFFFF0000)); // red

        // Fit both points on screen
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(currentLatLng)
                .include(emergencyLatLng)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150)); // 150px padding
    }

    private void fetchRouteAndDrawWithVolley() {
        String url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&destination=" + emergencyLatLng.latitude + "," + emergencyLatLng.longitude +
                "&mode=driving" +
                "&key=AIzaSyADB_W0m_bUfYumku4j_uczdtixkr6JZj4";  // Replace with your Google Directions API key

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONArray routes = json.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String encodedString = overviewPolyline.getString("points");

                            List<LatLng> points = PolylineDecoder.decodePolyline(encodedString);

                            runOnUiThread(() -> {
                                mMap.addPolyline(new PolylineOptions()
                                        .addAll(points)
                                        .width(10)
                                        .color(Color.BLUE));
                            });
                        } else {
                            Toast.makeText(EmergencyResponse.this, "No route found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(EmergencyResponse.this, "Parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(EmergencyResponse.this, "Failed to fetch route: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        queue.add(stringRequest);
    }
}
