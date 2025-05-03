package com.example.nexresq;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyActivity extends AppCompatActivity {

    private EditText userLocationEditText;
    private RecyclerView suggestionsRecyclerView;
    private SuggestionAdapter suggestionAdapter;
    private PlacesClient placesClient;

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


        selectLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent MapsActivity = new Intent(EmergencyActivity.this, MapsActivity.class);
                startActivity(MapsActivity);
            }
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("emergencyId")) {
            String emergencyId = intent.getStringExtra("emergencyId");
            if (emergencyId.equals("1"))
                textView.setText(" Medical Emergency");
            else if (emergencyId.equals("2"))
                textView.setText(" Fire Emergency");
            else if (emergencyId.equals("3"))
                textView.setText(" Police Emergency");

            double latitude = intent.getDoubleExtra("latitude", 0.0);
            double longitude = intent.getDoubleExtra("longitude", 0.0);

            String userLatLog = "Latitude: " + latitude + " & Longitude: " + longitude;
            userLatLogTextView.setText(userLatLog);

            // API request to update profile on server;
            String postUrl = GlobalData.BASE_URL+"emergency/create_emergency.php";
            Map<String, String> postParams = new HashMap<>();
            postParams.put("userId", GlobalData.getUserId(EmergencyActivity.this));
            postParams.put("serviceId",emergencyId);
            postParams.put("priority","Low");
            postParams.put("latitude",String.valueOf(latitude));
            postParams.put("longitude",String.valueOf(longitude));

            sendRequestLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    VolleyHelper.sendPostRequest(EmergencyActivity.this, postUrl, postParams, new VolleyHelper.VolleyCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Toast.makeText(EmergencyActivity.this, "Emergency request sent!", Toast.LENGTH_LONG).show();
                            Log.d("API_SUCCESS", response);
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(EmergencyActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                            Log.e("API_ERROR", error);
                        }
                    });
                }
            });
        }



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
}
