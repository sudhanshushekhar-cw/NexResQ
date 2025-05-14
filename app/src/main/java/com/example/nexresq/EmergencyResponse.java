package com.example.nexresq;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.nexresq.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EmergencyResponse extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_response);  // ✅ Only this line

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        Intent intent = getIntent();
        String userIdEme = intent.getStringExtra("userId");
        String latitudeEme = intent.getStringExtra("latitude");
        String longitudeEme = intent.getStringExtra("longitude");


        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("user")
                .child(userIdEme)
                .child("emergency");

        String add = longitudeEme + " , " + latitudeEme;
        TextView addressTextView = findViewById(R.id.addressTextView);
        Button acceptButton = findViewById(R.id.acceptButton);
        addressTextView.setText(add);

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> update = new HashMap<>();
                update.put("status", "Accepted");
                ref.updateChildren(update)
                        .addOnSuccessListener(aVoid -> {
                            // Optional: Toast or Log
                            Toast.makeText(EmergencyResponse.this, "Status updated to Accepted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(EmergencyResponse.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
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

        // Add a marker in Sydney and move the camera
        LatLng doon = new LatLng(30.2689, 77.9931);
        mMap.addMarker(new MarkerOptions().position(doon).title("About the location"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(doon));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(doon, 15));
    }
}