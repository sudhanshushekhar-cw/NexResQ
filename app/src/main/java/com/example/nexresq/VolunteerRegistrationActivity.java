package com.example.nexresq;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        Spinner spinnerServiceType = findViewById(R.id.spinnerServiceType);
        Spinner spinnerServiceTypeRequest = findViewById(R.id.spinnerServiceTypeRequest);

        String URL = GlobalData.BASE_URL+"service/get_all_services.php";
        String URL2 = GlobalData.BASE_URL+"organization/get_organizations.php";
        ArrayList<Model.Service> serviceList = new ArrayList<>();

        VolleyHelper.sendGetRequest(VolunteerRegistrationActivity.this, URL, new VolleyHelper.VolleyCallback() {
            @Override
            public void onSuccess(String response) {

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");
                    if(status.equals("success")){
                        JSONArray servicesArray = jsonObject.getJSONArray("services");

                        for(int i=0;i<servicesArray.length();i++){
                            JSONObject serviceObject = servicesArray.getJSONObject(i);
                            String serviceId = serviceObject.getString("serviceId");
                            String serviceName = serviceObject.getString("name");
                            serviceList.add(new Model.Service(serviceId,serviceName));
                        }

                        ArrayAdapter<Model.Service> adapter = new ArrayAdapter(VolunteerRegistrationActivity.this, android.R.layout.simple_spinner_dropdown_item, serviceList);
                        spinnerServiceType.setAdapter(adapter);
                        spinnerServiceTypeRequest.setAdapter(adapter);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(String error) {

            }
        });
        if (mode != null) {
            if (mode.equals("createOrg")) {
                findViewById(R.id.createOrgCard).setVisibility(View.VISIBLE);
            }else {
                findViewById(R.id.joinOrgCard).setVisibility(View.VISIBLE);
            }
        }
        Toast.makeText(this, "Mode: " + mode, Toast.LENGTH_SHORT).show();
    }
}