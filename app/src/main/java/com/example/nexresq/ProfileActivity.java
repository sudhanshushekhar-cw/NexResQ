package com.example.nexresq;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    EditText firstNameEditText, lastNameEditText, emailEditText, dobEditText;
    Spinner genderSpinner;
    Button submitBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        genderSpinner = findViewById(R.id.genderSpinner);
        emailEditText = findViewById(R.id.emailEditText);
        dobEditText = findViewById(R.id.dobEditText);
        submitBtn = findViewById(R.id.submitBtn);

        Calendar calendar = Calendar.getInstance();

        dobEditText.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    ProfileActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        // Format to yyyy-MM-dd
                        String formattedDob = year1 + "-" + String.format("%02d", month1 + 1) + "-" + String.format("%02d", dayOfMonth);
                        dobEditText.setText(formattedDob);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        ArrayList<String> genderList = new ArrayList<>();
        genderList.add("Select Gender");
        genderList.add("Male");
        genderList.add("Female");
        genderList.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);


        submitBtn.setOnClickListener(v -> {
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName = lastNameEditText.getText().toString().trim();
            String gender = genderSpinner.getSelectedItem().toString();
            String email = emailEditText.getText().toString().trim();
            String dob = dobEditText.getText().toString().trim();

            if (TextUtils.isEmpty(firstName)) {
                firstNameEditText.setError("First name is required");
                return;
            }

            if (TextUtils.isEmpty(lastName)) {
                lastNameEditText.setError("Last name is required");
                return;
            }

            if (gender.equals("Select Gender")) {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Enter a valid email address");
                return;
            }

            if (TextUtils.isEmpty(dob)) {
                dobEditText.setError("Date of birth is required");
                return;
            }

            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            String userId = sharedPreferences.getString("userId", "0");
            String number = sharedPreferences.getString("number", "0");

            // API request to update profile on server;
            String postUrl = GlobalData.BASE_URL+"nexresq/user/update_profile.php";
            Map<String, String> postParams = new HashMap<>();
            postParams.put("userId", userId);
            postParams.put("number", number);
            postParams.put("firstName", firstName);
            postParams.put("lastName", lastName);
            postParams.put("gender", gender);
            postParams.put("email", email);
            postParams.put("dob", dob);
            postParams.put("isProfileCompleted", "1");
            VolleyHelper.sendPostRequest(ProfileActivity.this, postUrl, postParams, new VolleyHelper.VolleyCallback() {
                @Override
                public void onSuccess(String response) {
//                  Log.d("POST Response", response);
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("firstName",firstName);
                    editor.putString("email",email);
                    editor.putString("isProfileCompleted","1");
                    editor.apply();
                    Toast.makeText(ProfileActivity.this, "Profile submitted successfully", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(ProfileActivity.this, HomeActivity.class);
                    startActivity(i);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(ProfileActivity.this, "Failed to submit", Toast.LENGTH_SHORT).show();
                    Log.e("POST Error", error);
                }
            });

        });

    }
}