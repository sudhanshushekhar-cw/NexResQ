package com.example.nexresq;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyActivity extends AppCompatActivity {

    private EditText userLocationEditText;
    private RecyclerView suggestionsRecyclerView;
    private SuggestionAdapter suggestionAdapter;
    private PlacesClient placesClient;
    private double finalLatitude, finalLongitude;
    private DatabaseReference refUser,ref;
    private String userId;
    private View sheetView;
    private ObjectAnimator progressAnimator;
    private BottomSheetDialog bottomSheetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        userId = GlobalData.getUserId(this);
        refUser = FirebaseDatabase.getInstance().getReference("user");
        ref = FirebaseDatabase.getInstance().getReference("user")
                .child(String.valueOf(userId))
                .child("emergency");


        ImageView backImageView = findViewById(R.id.backImageView);
        TextView textView = findViewById(R.id.textView);
        TextView userLatLogTextView = findViewById(R.id.userLatLogTextView);
        userLocationEditText = findViewById(R.id.userLocationEditText);
        suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView);
        Button selectLocationButton = findViewById(R.id.selectLocationButton);
        LinearLayout sendRequestLayout = findViewById(R.id.sendRequestLayout);

        backImageView.setOnClickListener(v -> finish());

        // Init Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);

        // Set up suggestions RecyclerView
        suggestionAdapter = new SuggestionAdapter(new ArrayList<>(), suggestion -> {
            userLocationEditText.setText(suggestion.getPrimaryText(null).toString());
            suggestionsRecyclerView.setVisibility(View.GONE);
        });

        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecyclerView.setAdapter(suggestionAdapter);

        userLocationEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fetchSuggestions(s.toString());
            }
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("emergencyId")) {
            String emergencyId = intent.getStringExtra("emergencyId");

            switch (emergencyId) {
                case "1": textView.setText("Medical Emergency"); break;
                case "2": textView.setText("Fire Emergency"); break;
                case "3": textView.setText("Police Emergency"); break;
            }

            finalLatitude = intent.getDoubleExtra("latitude", 0.0);
            finalLongitude = intent.getDoubleExtra("longitude", 0.0);
            userLatLogTextView.setText("Latitude: " + finalLatitude + " & Longitude: " + finalLongitude);

            String postUrl = GlobalData.BASE_URL + "emergency/create_emergency.php";
            Map<String, String> postParams = new HashMap<>();
            postParams.put("userId", GlobalData.getUserId(EmergencyActivity.this));
            postParams.put("serviceId", emergencyId);
            postParams.put("priority", "Low");
            postParams.put("latitude", String.valueOf(finalLatitude));
            postParams.put("longitude", String.valueOf(finalLongitude));

            //update in realtime database
            Map<String, Object> emergencyData = new HashMap<>();
            emergencyData.put("last_updated", System.currentTimeMillis());
            emergencyData.put("latitude", String.valueOf(finalLatitude));
            emergencyData.put("longitude", String.valueOf(finalLongitude));
            emergencyData.put("serviceId", emergencyId);
            emergencyData.put("status", "Pending");

            sendRequestLayout.setOnClickListener(v -> {
                sheetView =  LayoutInflater.from(getApplicationContext()).inflate(R.layout.bottom_sheet_vol_search, null);
                showBottomSheet(sheetView, true);
                VolleyHelper.sendPostRequest(EmergencyActivity.this, postUrl, postParams, new VolleyHelper.VolleyCallback() {
                    @Override
                    public void onSuccess(String response) {
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(response);
                            String emergencyId = jsonObject.getString("emergencyId");
                            emergencyData.put("emergencyId", emergencyId);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        ref.setValue(emergencyData).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                trackAcceptance();
                                Log.d("FirebaseDB", "Emergency data updated successfully in Realtime Database.");
                                Toast.makeText(EmergencyActivity.this, "Emergency request sent!", Toast.LENGTH_LONG).show();
                            } else {
                                Log.e("FirebaseDB", "Failed to update emergency data: " + task.getException());
                                Toast.makeText(EmergencyActivity.this, "Failed to update emergency data", Toast.LENGTH_LONG).show();
                            }
                        });

                        Toast.makeText(EmergencyActivity.this, "Emergency request sent!", Toast.LENGTH_LONG).show();
                        Log.d("API_SUCCESS", response);
//                        findNearestVolunteers(finalLatitude, finalLongitude, emergencyId); // Call here after success
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(EmergencyActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        Log.e("API_ERROR", error);
                    }
                });
            });

        }
    }

    private void trackAcceptance(){
        ref.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if ("Accepted".equalsIgnoreCase(status)) {
                        View newSheetView = LayoutInflater.from(EmergencyActivity.this).inflate(R.layout.bottom_sheet_vol_search, null);
                        showBottomSheet(newSheetView, false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmergencyActivity.this, "Failed to read status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSuggestions(String query) {
        if (query.isEmpty()) {
            suggestionsRecyclerView.setVisibility(View.GONE);
            return;
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountry("IN")
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    if (predictions.isEmpty()) {
                        suggestionsRecyclerView.setVisibility(View.GONE);
                    } else {
                        suggestionAdapter.updateData(predictions);
                        suggestionsRecyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Places", "Prediction fetch error", e);
                    Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show();
                });
    }

    private void showBottomSheet(View sheetView, Boolean isProgress) {
        if (bottomSheetDialog == null) {
            bottomSheetDialog = new BottomSheetDialog(EmergencyActivity.this);
            bottomSheetDialog.setContentView(sheetView);
        } else {
            bottomSheetDialog.setContentView(sheetView);
        }

        View bottomSheet = bottomSheetDialog.getDelegate().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackground(null);
        }

        LottieAnimationView animationView = sheetView.findViewById(R.id.lottieLoadingAnimation);
        ProgressBar progressBar = sheetView.findViewById(R.id.progressBarHorizontal);
        animationView.setAnimation(R.raw.loading);
        animationView.playAnimation();

        if (isProgress) {
            if (!bottomSheetDialog.isShowing()) {
                bottomSheetDialog.show();
            }
            startProgressAnimation(progressBar);
        } else {
            animationView.setAnimation(R.raw.accepted);
            stopProgressAnimation(progressBar);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
                    bottomSheetDialog.dismiss();
                }
            }, 3000);
        }
    }


    private void startProgressAnimation(ProgressBar progressBar) {
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnimator.setDuration(20000);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        progressAnimator.start();
    }

    private void stopProgressAnimation(ProgressBar progressBar) {
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressBar.setProgress(100);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(EmergencyActivity.this, MapsActivity.class);
                intent.putExtra("userIdEme",GlobalData.getUserId(EmergencyActivity.this));
                intent.putExtra("isGeofencingFeature",false);
                startActivity(intent);
                finish(); // optional
            }, 3000);
        }
    }


    // Start volunteer search
    public void findNearestVolunteers(double userLat, double userLng, String serviceId) {
        searchForVolunteers(userLat, userLng, 2000, serviceId);
    }

    private void searchForVolunteers(double userLat, double userLng, int radiusMeters, String emergencyServiceId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> nearbyVolunteers = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Boolean isVolunteer = userSnapshot.child("isVolunteer").getValue(Boolean.class);
                    Boolean isAvailable = userSnapshot.child("isAvailable").getValue(Boolean.class);
                    Boolean onDuty = userSnapshot.child("onDuty").getValue(Boolean.class);
                    String volunteerServiceId = userSnapshot.child("serviceId").getValue(String.class);

                    if (isVolunteer != null && isVolunteer &&
                            isAvailable != null && isAvailable &&
                            onDuty != null && onDuty &&
                            volunteerServiceId != null && volunteerServiceId.equals(emergencyServiceId)) {

                        Double lat = userSnapshot.child("locations").child("latitude").getValue(Double.class);
                        Double lng = userSnapshot.child("locations").child("longitude").getValue(Double.class);

                        if (lat != null && lng != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(userLat, userLng, lat, lng, results);
                            if (results[0] <= radiusMeters) {
                                nearbyVolunteers.add(userSnapshot.getKey());
                            }
                        }
                    }
                }

                if (!nearbyVolunteers.isEmpty()) {
                    Log.d("EMERGENCY", "Volunteers found: " + nearbyVolunteers.size());
                    sendNotificationsToVolunteers(nearbyVolunteers);
                } else if (radiusMeters < 10000) {
                    new Handler().postDelayed(() -> {
                        searchForVolunteers(userLat, userLng, radiusMeters + 3000, emergencyServiceId);
                    }, 10000);
                } else {
                    Toast.makeText(EmergencyActivity.this, "No volunteer found nearby.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error reading user data", error.toException());
            }
        });
    }


    private void sendNotificationsToVolunteers(List<String> volunteerIds) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user");
        for (String userId : volunteerIds) {
            userRef.child(userId).child("fcmTokens").get().addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    String token = dataSnapshot.getValue(String.class);
                    sendNotificationToToken(token, "Emergency Alert", "Someone nearby needs help!");
                }
            });
        }
    }

    public void sendNotificationToToken(String fcmToken, String title, String message) {
        String serverKey = "YOUR_SERVER_KEY_HERE"; // Replace with your actual FCM server key

        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject json = new JSONObject();
            json.put("to", fcmToken);

            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", message);

            json.put("notification", notification);
            json.put("priority", "high");

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                    response -> Log.d("FCM", "Notification Sent: " + response),
                    error -> Log.e("FCM", "Error Sending Notification", error)
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "key=" + serverKey);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
