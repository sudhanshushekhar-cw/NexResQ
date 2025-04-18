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
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.appcompat.app.AlertDialog;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FusedLocationProviderClient fusedLocationClient;
    private String selectedEmergencyType;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startLocationService();
                    if (selectedEmergencyType != null) {
                        fetchLocationAndLaunchActivity(selectedEmergencyType);
                    }
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        CardView medicalCard = view.findViewById(R.id.medicalCard);
        CardView fireCard = view.findViewById(R.id.fireCard);
        CardView policeCard = view.findViewById(R.id.policeCard);
        CardView becomeVolunteerCard = view.findViewById(R.id.becomeVolunteerCard);

        // Check permission once on load
        requestLocationPermission(null);

        medicalCard.setOnClickListener(v -> requestLocationPermission("Medical Emergency"));
        fireCard.setOnClickListener(v -> requestLocationPermission("Fire Emergency"));
        policeCard.setOnClickListener(v -> requestLocationPermission("Police Emergency"));

        becomeVolunteerCard.setOnClickListener(v -> showBottomSheet());

        return view;
    }

    private void requestLocationPermission(String emergencyType) {
        selectedEmergencyType = emergencyType;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startLocationService();

            if (emergencyType != null) {
                fetchLocationAndLaunchActivity(emergencyType);
            }
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(getActivity(), LocationService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
    }

    private void fetchLocationAndLaunchActivity(String emergencyType) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        launchEmergencyActivity(emergencyType, location);
                    } else {
                        Toast.makeText(getContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to get location", Toast.LENGTH_SHORT).show();
                });
    }

    private void launchEmergencyActivity(String type, Location location) {
        Intent intent = new Intent(getActivity(), EmergencyActivity.class);
        intent.putExtra("emergencyType", type);
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        startActivity(intent);
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permission Needed")
                .setMessage("Please enable location permission in settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_vol_reg, null);
        bottomSheetDialog.setContentView(sheetView);

        View bottomSheet = bottomSheetDialog.getDelegate().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackground(null);
        }

        bottomSheetDialog.show();

        Button createOrgButton = sheetView.findViewById(R.id.createOrgButton);
        Button joinOrgButton = sheetView.findViewById(R.id.joinOrgButton);

        Intent intent = new Intent(getActivity(), VolunteerRegistrationActivity.class);
        createOrgButton.setOnClickListener(v -> {
            intent.putExtra("mode", "createOrg");
            startActivity(intent);
        });

        joinOrgButton.setOnClickListener(v -> {
            intent.putExtra("mode", "joinOrg");
            startActivity(intent);
        });
    }
}
