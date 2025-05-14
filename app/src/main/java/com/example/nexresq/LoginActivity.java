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

import com.google.firebase.BuildConfig;
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

    private FirebaseAuth mAuth;
    private EditText editTextPhone, editTextOtp;
    private Button btnNext, btnVerify;
    private TextView textViewTitle, textViewSubTitle;
    private ProgressDialog progressDialog;
    private String verificationId;
    private static final String TAG = "LoginActivity";

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        btnNext.setOnClickListener(view -> {
            String phone = editTextPhone.getText().toString().trim();
            if (!phone.isEmpty() && phone.length() == 10) {
                String fullPhone = "+91" + phone;
                updateUIForOTP(fullPhone);
                sendVerificationCode(fullPhone);
            } else {
                textViewSubTitle.setText("Mobile number is not valid");
                textViewSubTitle.setTextColor(Color.RED);
                textViewSubTitle.setVisibility(View.VISIBLE);
            }
        });

        btnVerify.setOnClickListener(view -> {
            String otp = editTextOtp.getText().toString().trim();
            if (!otp.isEmpty() && otp.length() == 6) {
                progressDialog.show();
                verifyCode(otp);
            } else {
                textViewSubTitle.setText("OTP is not valid");
                textViewSubTitle.setTextColor(Color.RED);
                textViewSubTitle.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateUIForOTP(String fullPhone) {
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
        if (BuildConfig.DEBUG) {
            // Fake test verification ID for Firebase testing numbers
            verificationId = "TEST_VERIFICATION_ID";
            textViewSubTitle.setText("Enter the test OTP (set in Firebase console)");
            textViewSubTitle.setTextColor(Color.GRAY);
            textViewSubTitle.setVisibility(View.VISIBLE);
            return;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallBack)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
            super.onCodeSent(s, token);
            verificationId = s;
        }

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            String code = credential.getSmsCode();
            if (code != null) {
                editTextOtp.setText(code);
                verifyCode(code);
            }
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            Log.e(TAG, "Verification failed: " + e.getMessage());
            Toast.makeText(LoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    private void verifyCode(String code) {
        PhoneAuthCredential credential;

        if (BuildConfig.DEBUG) {
            credential = PhoneAuthProvider.getCredential("TEST_VERIFICATION_ID", code);
        } else {
            credential = PhoneAuthProvider.getCredential(verificationId, code);
        }

        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String number = editTextPhone.getText().toString();
                String postUrl = GlobalData.BASE_URL + "user/add_user_number.php";

                Map<String, String> postParams = new HashMap<>();
                postParams.put("number", number);

                VolleyHelper.sendPostRequest(this, postUrl, postParams, new VolleyHelper.VolleyCallback() {
                    @Override
                    public void onSuccess(String response) {
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

                            updateFirebaseUser(userObject.getString("userId"), userObject.getString("isVolunteer"), userObject.getString("serviceId"));

                            progressDialog.dismiss();
                            if (userObject.getString("isProfileCompleted").equals("0")) {
                                startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                            } else {
                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            }
                            finish();

                        } catch (JSONException e) {
                            progressDialog.dismiss();
                            Log.e(TAG, "JSON error: ", e);
                            Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Volley error: " + error);
                        Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                progressDialog.dismiss();
                Log.e(TAG, "Sign-in error: ", task.getException());
                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirebaseUser(String userId, String isVolunteer,  String serviceId) {
        Map<String, Object> userData = new HashMap<>();
        if(isVolunteer.equals("1")){
            userData.put("isAvailable", true);
        }else{
            userData.put("isAvailable", false);
        }
        userData.put("isVolunteer", isVolunteer.equals("1"));
        userData.put("serviceId", serviceId);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                userData.put("fcmTokens", token);
                FirebaseDatabase.getInstance().getReference("user").child(userId).setValue(userData);
                Log.d(TAG, "FCM token updated for user");
            } else {
                Log.w(TAG, "FCM token fetch failed");
            }
        });
    }
}
