package com.example.nexresq;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FusedLocationProviderClient fusedLocationClient;
    private String emergencyTypeToLaunch = null;
    double lat,lon;

    // Write a message to the database
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("message");



    // ✅ Activity Result Launcher for location permission
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // ✅ Permission granted now — call this to start service & activity
//                    fetchLocationAndLaunchActivity();
                    Intent serviceIntent = new Intent(getActivity(), LocationService.class);
                    ContextCompat.startForegroundService(requireContext(), serviceIntent);
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showSettingsDialog();
                    } else {
                        Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        // ✅ Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        CardView medicalCard = view.findViewById(R.id.medicalCard);
        CardView fireCard = view.findViewById(R.id.fireCard);
        CardView policeCard = view.findViewById(R.id.policeCard);

        requestLocationWithPermission();
        Intent intent = new Intent(getActivity(), EmergencyActivity.class);
        medicalCard.setOnClickListener(v -> {
//            myRef.setValue("Hello, World!");
            // ✅ Start LocationService

            emergencyTypeToLaunch = "Medical Emergency";
            requestLocationWithPermission();
            intent.putExtra("emergencyType", emergencyTypeToLaunch);
            intent.putExtra("latitude", lat);
            intent.putExtra("longitude", lon);
            startActivity(intent);
        });

        fireCard.setOnClickListener(v -> {
            emergencyTypeToLaunch = "Fire Emergency";
            requestLocationWithPermission();
            intent.putExtra("emergencyType", emergencyTypeToLaunch);
            intent.putExtra("latitude", lat);
            intent.putExtra("longitude", lon);
            startActivity(intent);
        });

        policeCard.setOnClickListener(v -> {
            emergencyTypeToLaunch = "Police Emergency";
            requestLocationWithPermission();
            intent.putExtra("emergencyType", emergencyTypeToLaunch);
            intent.putExtra("latitude", lat);
            intent.putExtra("longitude", lon);
            startActivity(intent);
        });

        return view;
    }

    // ✅ Check and request location permission
    private void requestLocationWithPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(getContext(), "Location permission is needed for emergency features.", Toast.LENGTH_LONG).show();
            }

            // Launch permission dialog
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        } else {
            Intent serviceIntent = new Intent(getActivity(), LocationService.class);
            ContextCompat.startForegroundService(requireContext(), serviceIntent);
        }
    }

    // ✅ Fetch location and start EmergencyActivity
    private void fetchLocationAndLaunchActivity() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        lat = location.getLatitude();
                        lon = location.getLongitude();
                        Log.d(TAG, "Location: " + lat + ", " + lon);
                    } else {
                        Toast.makeText(getContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to get location", Toast.LENGTH_SHORT).show();
                });
    }

    // ⚙️ Dialog to guide user to app settings when permission is permanently denied
    private void showSettingsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Location Permission Needed")
                .setMessage("Please enable location permission in app settings to continue.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
