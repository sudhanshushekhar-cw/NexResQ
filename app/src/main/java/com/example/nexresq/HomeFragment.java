package com.example.nexresq;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FusedLocationProviderClient fusedLocationClient;
    private String selectedEmergencyType;

    private LinearLayout emergencyStatusLinearLayout;
    private TextView volunteerNameTextView;
    private ImageView callIcon;
    private TextView emergencyIdTextView;
    private CardView emergencyStatusCard;

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
    public void onResume() {
        super.onResume();
        refreshCardDataForUser();  // 👈 Custom method to update card data
        refreshCardDataForVolunteer();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE);

        String isVolunteer = GlobalData.isVolunteer(requireContext());
        String isOrganization = GlobalData.isOrganization(requireContext());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        CardView medicalCard = view.findViewById(R.id.medicalCard);
        CardView fireCard = view.findViewById(R.id.fireCard);
        CardView policeCard = view.findViewById(R.id.policeCard);
        CardView becomeVolunteerCard = view.findViewById(R.id.becomeVolunteerCard);


        emergencyStatusLinearLayout = view.findViewById(R.id.emergencyStatusLinearLayout);
        volunteerNameTextView = view.findViewById(R.id.volunteerNameTextView);
        callIcon = view.findViewById(R.id.callIcon);
        emergencyIdTextView = view.findViewById(R.id.emergencyIdTextView);
        emergencyStatusCard = view.findViewById(R.id.emergencyStatusCard);

        // Check permission once on load
        requestLocationPermission(null);

        if(isOrganization.equals("1")){
            becomeVolunteerCard.setVisibility(View.GONE);
        }

        medicalCard.setOnClickListener(v -> requestLocationPermission("1"));
        fireCard.setOnClickListener(v -> requestLocationPermission("2"));
        policeCard.setOnClickListener(v -> requestLocationPermission("3"));

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
        startEmergencyListenerService();
    }

    private void startEmergencyListenerService() {
        Intent serviceIntent = new Intent(requireContext(), EmergencyListenerService.class);
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
        intent.putExtra("emergencyId", type);
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

    private void refreshCardDataForUser() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("user")
                .child(GlobalData.getUserId(requireContext())).child("emergency");  // Or your correct node

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String volunteerId = snapshot.child("volunteerId").getValue(String.class);
                    String emergencyId = snapshot.child("emergencyId").getValue(String.class);
                    String POST_URL = GlobalData.BASE_URL + "user/get_user_details.php";
                    Map<String, String> params = new HashMap<>();
                    params.put("userId", volunteerId);
                    VolleyHelper.sendPostRequest(requireContext(), POST_URL, params, new VolleyHelper.VolleyCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d(TAG, "Response userr: " + response);
                            try {
                                JSONObject jsonObject = new JSONObject(response);

                                String statusCode = jsonObject.getString("status");
                                if ("success".equalsIgnoreCase(statusCode)) {
                                    JSONObject dataObject = jsonObject.getJSONObject("data");

                                    String firstName = dataObject.getString("firstName");
                                    String lastName = dataObject.getString("lastName");
                                    String volunteerPhoneNumber = dataObject.getString("number");

                                    String fullName = firstName + " " + lastName;
                                    volunteerNameTextView.setText(fullName);
                                    emergencyIdTextView.setText("#" + emergencyId);

                                    if ("Accepted".equals(status)) {
                                        emergencyStatusLinearLayout.setVisibility(View.VISIBLE);
                                        // Set click listener to open dialer
                                        callIcon.setOnClickListener(v -> {
                                            if (volunteerPhoneNumber != null && !volunteerPhoneNumber.isEmpty()) {
                                                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                                                dialIntent.setData(Uri.parse("tel:" + volunteerPhoneNumber));
                                                startActivity(dialIntent);
                                            } else {
                                                Toast.makeText(requireContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                        emergencyStatusCard.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Intent intent = new Intent(requireContext(), MapsActivity.class);
                                                intent.putExtra("userIdEme",GlobalData.getUserId(requireContext()));
                                                intent.putExtra("isGeofencingFeature",false);
                                                startActivity(intent);
                                            }
                                        });

                                    } else {
                                        emergencyStatusLinearLayout.setVisibility(View.GONE);
                                    }
                                } else {
                                    Log.e(TAG, "Failed to fetch user data: " + jsonObject.getString("message"));
                                }

                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            }
                        }


                        @Override
                        public void onError(String error) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshCardDataForVolunteer() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("user")
                .child(GlobalData.getUserId(requireContext()));  // Or your correct node

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userIdEme = snapshot.child("userIdEme").getValue(String.class);
                    Boolean isAvailable = snapshot.child("isAvailable").getValue(Boolean.class);
                    String emergencyStatus = snapshot.child("emergencyStatus").getValue(String.class);
                    String emergencyId = snapshot.child("emergencyId").getValue(String.class);
                    String POST_URL = GlobalData.BASE_URL + "user/get_user_details.php";
                    Map<String, String> params = new HashMap<>();
                    params.put("userId", userIdEme);
                    VolleyHelper.sendPostRequest(requireContext(), POST_URL, params, new VolleyHelper.VolleyCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d(TAG, "Response userr: " + response);
                            try {
                                JSONObject jsonObject = new JSONObject(response);

                                String statusCode = jsonObject.getString("status");
                                if ("success".equalsIgnoreCase(statusCode)) {
                                    JSONObject dataObject = jsonObject.getJSONObject("data");

                                    String firstName = dataObject.getString("firstName");
                                    String lastName = dataObject.getString("lastName");
                                    String volunteerPhoneNumber = dataObject.getString("number");

                                    String fullName = firstName + " " + lastName;
                                    volunteerNameTextView.setText(fullName);
                                    emergencyIdTextView.setText("#" + emergencyId);

                                    if (!isAvailable && "Accepted".equals(emergencyStatus)) {
                                        emergencyStatusLinearLayout.setVisibility(View.VISIBLE);
                                        // Set click listener to open dialer
                                        callIcon.setOnClickListener(v -> {
                                            if (volunteerPhoneNumber != null && !volunteerPhoneNumber.isEmpty()) {
                                                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                                                dialIntent.setData(Uri.parse("tel:" + volunteerPhoneNumber));
                                                startActivity(dialIntent);
                                            } else {
                                                Toast.makeText(requireContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                        emergencyStatusCard.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Intent intent = new Intent(requireContext(), MapsActivity.class);
                                                intent.putExtra("userIdEme",userIdEme);
                                                intent.putExtra("isGeofencingFeature",true);
                                                startActivity(intent);
                                            }
                                        });

                                    } else {
                                        emergencyStatusLinearLayout.setVisibility(View.GONE);
                                    }
                                } else {
                                    Log.e(TAG, "Failed to fetch user data: " + jsonObject.getString("message"));
                                }

                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            }
                        }


                        @Override
                        public void onError(String error) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
