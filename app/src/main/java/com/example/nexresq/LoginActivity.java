package com.example.nexresq;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivityPro";

    private FirebaseAuth mAuth;
    private EditText editTextPhone, editTextOtp;
    private Button btnNext, btnVerify;
    private TextView textViewTitle, textViewSubTitle;
    private ProgressDialog progressDialog;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading, please wait...");
        progressDialog.setCancelable(false);

        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSubTitle = findViewById(R.id.textViewSubTitle);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOtp = findViewById(R.id.editTextOtp);
        btnNext = findViewById(R.id.btnNext);
        btnVerify = findViewById(R.id.btnVerify);

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        btnNext.setOnClickListener(view -> {
            String phone = editTextPhone.getText().toString().trim();
            Log.d(TAG, "Next button clicked with phone: " + phone);
            if (!phone.isEmpty() && phone.length() == 10) {
                String fullPhone = "+91" + phone;
                Log.d(TAG, "Sending verification code to: " + fullPhone);
                updateUIForOTP(fullPhone);
                sendVerificationCode(fullPhone);
            } else {
                Log.w(TAG, "Invalid phone number entered");
                textViewSubTitle.setText("Mobile number is not valid");
                textViewSubTitle.setTextColor(Color.RED);
                textViewSubTitle.setVisibility(View.VISIBLE);
            }
        });

        btnVerify.setOnClickListener(view -> {
            String otp = editTextOtp.getText().toString().trim();
            Log.d(TAG, "Verify button clicked with OTP: " + otp);
            if (!otp.isEmpty() && otp.length() == 6) {
                progressDialog.show();
                verifyCode(otp);
            } else {
                Log.w(TAG, "Invalid OTP entered");
                textViewSubTitle.setText("OTP is not valid");
                textViewSubTitle.setTextColor(Color.RED);
                textViewSubTitle.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateUIForOTP(String fullPhone) {
        Log.d(TAG, "Updating UI to show OTP input for phone: " + fullPhone);
        textViewTitle.setText("Enter verification code");
        textViewSubTitle.setText("Sent to " + fullPhone);
        textViewSubTitle.setTextColor(Color.GRAY);
        textViewSubTitle.setVisibility(View.VISIBLE);
        editTextPhone.setVisibility(View.GONE);
        editTextOtp.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
        btnVerify.setVisibility(View.VISIBLE);
    }

    private void sendVerificationCode(String phoneNumber) {
        Log.d(TAG, "sendVerificationCode called for phoneNumber: " + phoneNumber);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallBack)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBack =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    super.onCodeSent(s, token);
                    Log.d(TAG, "onCodeSent: verificationId = " + s);
                    verificationId = s;
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    Log.d(TAG, "onVerificationCompleted called");
                    String code = credential.getSmsCode();
                    if (code != null) {
                        Log.d(TAG, "Auto-retrieved OTP: " + code);
                        editTextOtp.setText(code);
                        verifyCode(code);
                    } else {
                        Log.d(TAG, "Verification completed without SMS code");
                    }
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Verification failed: " + e.getMessage(), e);
                    Toast.makeText(LoginActivity.this,
                            "Verification failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            };

    private void verifyCode(String code) {
        Log.d(TAG, "verifyCode called with code: " + code);
        if (verificationId == null) {
            Log.w(TAG, "verificationId is null; cannot verify code");
            Toast.makeText(this, "Verification ID missing. Please resend OTP.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        Log.d(TAG, "signInWithCredential called");
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithCredential successful");
                Toast.makeText(LoginActivity.this, "Authentication successful", Toast.LENGTH_SHORT).show();

                String number = editTextPhone.getText().toString();
                Log.d(TAG, "Logged in user number: " + number);

                String postUrl = GlobalData.BASE_URL + "user/add_user_number.php";

                Map<String, String> postParams = new HashMap<>();
                postParams.put("number", number);

                VolleyHelper.sendPostRequest(this, postUrl, postParams, new VolleyHelper.VolleyCallback() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d(TAG, "Server response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject userObject = jsonObject.getJSONObject("user");

                            SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("number", number);
                            editor.putString("firstName", userObject.getString("firstName"));
                            editor.putString("lastName", userObject.getString("lastName"));
                            editor.putString("email", userObject.getString("email"));
                            editor.putString("userId", userObject.getString("userId"));
                            editor.putString("isProfileCompleted", userObject.getString("isProfileCompleted"));
                            editor.putString("isVolunteer", userObject.getString("isVolunteer"));
                            editor.putString("organizationId", userObject.getString("organizationId"));
                            editor.putString("isOrganization", userObject.getString("isOrganization"));
                            editor.apply();

                            Log.d(TAG, "User data saved to SharedPreferences");

                            updateFirebaseUser(userObject.getString("userId"),
                                    userObject.getString("isVolunteer"),
                                    userObject.getString("serviceId"));

                            if (userObject.getString("isProfileCompleted").equals("0")) {
                                Log.d(TAG, "Profile incomplete. Redirecting to ProfileActivity.");
                                startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                            } else {
                                Log.d(TAG, "Profile complete. Redirecting to HomeActivity.");
                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            }
                            finish();

                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error", e);
                            Toast.makeText(LoginActivity.this, "Login failed due to server error", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Volley error: " + error);
                        Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                Log.e(TAG, "Authentication failed", task.getException());
                Toast.makeText(LoginActivity.this,
                        "Authentication failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateFirebaseUser(String userId, String isVolunteer, String serviceId) {
        Log.d(TAG, "Updating Firebase user data for userId: " + userId);
        Map<String, Object> userData = new HashMap<>();
        userData.put("isAvailable", "1".equals(isVolunteer));
        userData.put("isVolunteer", "1".equals(isVolunteer));
        userData.put("onDuty", false);
        userData.put("serviceId", serviceId);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d(TAG, "FCM token fetched: " + token);
                userData.put("fcmTokens", token);
                FirebaseDatabase.getInstance().getReference("user").child(userId).setValue(userData)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.d(TAG, "User data updated successfully in Firebase Database");
                            } else {
                                Log.e(TAG, "Failed to update user data in Firebase Database", task1.getException());
                            }
                        });
            } else {
                Log.w(TAG, "Failed to fetch FCM token", task.getException());
            }
        });
    }
}
