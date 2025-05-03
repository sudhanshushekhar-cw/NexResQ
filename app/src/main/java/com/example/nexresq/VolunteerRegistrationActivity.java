package com.example.nexresq;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VolunteerRegistrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_volunteer_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String userId = GlobalData.getUserId(this);
        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");

        // UI References
        Spinner spinnerServiceType = findViewById(R.id.spinnerServiceType);
        EditText editTextName = findViewById(R.id.editTextName);
        EditText editTextNumber = findViewById(R.id.editTextNumber);
        EditText editTextEmail = findViewById(R.id.editTextEmail);
        EditText editTextAreaCode = findViewById(R.id.editTextAreaCode);
        Button btnCreateOrg = findViewById(R.id.btnCreateOrg);

        Spinner spinnerServiceTypeRequest = findViewById(R.id.spinnerServiceTypeRequest);
        Spinner spinnerOrganization = findViewById(R.id.spinnerOrganization);
        EditText editTextNameRequest = findViewById(R.id.editTextNameRequest);
        Button btnRequestApproval = findViewById(R.id.btnRequestApproval);

        editTextNameRequest.setText(GlobalData.getFirstName(this) + " " + GlobalData.getLastName(this));

        // Show correct card
        if ("createOrg".equals(mode)) {
            findViewById(R.id.createOrgCard).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.joinOrgCard).setVisibility(View.VISIBLE);
        }

        // Fetch services
        ArrayList<Model.Service> serviceList = new ArrayList<>();
        String serviceURL = GlobalData.BASE_URL + "service/get_all_services.php";
        VolleyHelper.sendGetRequest(this, serviceURL, new VolleyHelper.VolleyCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has("status") && jsonObject.getString("status").equals("success")) {
                        JSONArray servicesArray = jsonObject.getJSONArray("services");
                        for (int i = 0; i < servicesArray.length(); i++) {
                            JSONObject serviceObj = servicesArray.getJSONObject(i);
                            serviceList.add(new Model.Service(serviceObj.getString("serviceId"), serviceObj.getString("name")));
                        }

                        ArrayAdapter<Model.Service> adapter = new ArrayAdapter<>(VolunteerRegistrationActivity.this, android.R.layout.simple_spinner_dropdown_item, serviceList);
                        spinnerServiceType.setAdapter(adapter);
                        spinnerServiceTypeRequest.setAdapter(adapter);
                    }
                } catch (JSONException e) {
                    Log.e("JSON_ERROR", "Parsing services: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(VolunteerRegistrationActivity.this, "Service fetch failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Organization Creation
        String createOrgUrl = GlobalData.BASE_URL + "organization/create_organization.php";
        btnCreateOrg.setOnClickListener(v -> {
            Map<String, String> postParams = new HashMap<>();
            Model.Service selectedService = (Model.Service) spinnerServiceType.getSelectedItem();

            postParams.put("userId", userId);
            postParams.put("serviceId", selectedService.getId());
            postParams.put("name", editTextName.getText().toString());
            postParams.put("number", editTextNumber.getText().toString());
            postParams.put("email", editTextEmail.getText().toString());
            postParams.put("areaCode", editTextAreaCode.getText().toString());

            VolleyHelper.sendPostRequest(this, createOrgUrl, postParams, new VolleyHelper.VolleyCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getString("status").equals("success")) {
                            JSONObject orgObj = jsonObject.getJSONObject("organization");
                            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("isOrganization", "1");
                            editor.putString("organizationId", orgObj.getString("organizationId"));
                            editor.apply();

                            Toast.makeText(VolunteerRegistrationActivity.this, "Organization created successfully", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("refresh", true);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(VolunteerRegistrationActivity.this, "Response Error", Toast.LENGTH_SHORT).show();
                        Log.e("JSON_ERROR", "Org Create: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(VolunteerRegistrationActivity.this, "Creation failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Fetch organizations by service
        String getOrgsUrl = GlobalData.BASE_URL + "organization/get_organizations_by_service.php";
        spinnerServiceTypeRequest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Model.Service service = (Model.Service) spinnerServiceTypeRequest.getSelectedItem();
                Map<String, String> postParams = new HashMap<>();
                postParams.put("serviceId", service.getId());

                VolleyHelper.sendPostRequest(VolunteerRegistrationActivity.this, getOrgsUrl, postParams, new VolleyHelper.VolleyCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                JSONArray orgsArray = jsonObject.getJSONArray("data");
                                ArrayList<Model.Organization> orgList = new ArrayList<>();

                                for (int i = 0; i < orgsArray.length(); i++) {
                                    JSONObject org = orgsArray.getJSONObject(i);
                                    orgList.add(new Model.Organization(org.getString("organizationId"), org.getString("name")));
                                }

                                ArrayAdapter<Model.Organization> adapter = new ArrayAdapter<>(VolunteerRegistrationActivity.this, android.R.layout.simple_spinner_dropdown_item, orgList);
                                spinnerOrganization.setAdapter(adapter);
                            }
                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Org List: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(VolunteerRegistrationActivity.this, "Organization fetch error", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Join organization
        String joinOrgUrl = GlobalData.BASE_URL + "user/update_user_organization_service.php";
        btnRequestApproval.setOnClickListener(v -> {
            if (spinnerServiceTypeRequest.getSelectedItem() == null || spinnerOrganization.getSelectedItem() == null) {
                Toast.makeText(this, "Select both service and organization", Toast.LENGTH_SHORT).show();
                return;
            }

            Model.Service service = (Model.Service) spinnerServiceTypeRequest.getSelectedItem();
            Model.Organization organization = (Model.Organization) spinnerOrganization.getSelectedItem();

            Map<String, String> postParams = new HashMap<>();
            postParams.put("userId", userId);
            postParams.put("serviceId", service.getId());
            postParams.put("organizationId", organization.getId());

            VolleyHelper.sendPostRequest(this, joinOrgUrl, postParams, new VolleyHelper.VolleyCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getString("status").equals("success")) {
                            Toast.makeText(VolunteerRegistrationActivity.this, "Joined successfully", Toast.LENGTH_SHORT).show();

                            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("isVolunteer", "1");
                            editor.putString("organizationId", organization.getId());
                            editor.apply();

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("refresh", true);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Join Org: " + e.getMessage());
                        Toast.makeText(VolunteerRegistrationActivity.this, "Join failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(VolunteerRegistrationActivity.this, "Join request failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
