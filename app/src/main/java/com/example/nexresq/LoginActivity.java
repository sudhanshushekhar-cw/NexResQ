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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class LoginActivity extends AppCompatActivity {

    // variable for FirebaseAuth class
    private FirebaseAuth mAuth;

    private TextView textViewSubTitle, textViewTitle;
    private EditText editTextPhone, editTextOtp;
    private Button btnNext, btnVerify;

    private ProgressDialog progressDialog;

    private String verificationId;
    private DatabaseReference ref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Loading, please wait...");
        progressDialog.setCancelable(false);


        // below line is for getting instance
        // of our FirebaseAuth.
        mAuth = FirebaseAuth.getInstance();

        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSubTitle = findViewById(R.id.textViewSubTitle);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOtp = findViewById(R.id.editTextOtp);
        btnNext = findViewById(R.id.btnNext);
        btnVerify = findViewById(R.id.btnVerify);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!editTextPhone.getText().toString().isEmpty() && editTextPhone.getText().toString().length() == 10){
                    String phoneNumber = "+91"+editTextPhone.getText().toString();
                    textViewTitle.setText("Enter verification code");
                    textViewSubTitle.setText("Sent to +91 "+ phoneNumber);
                    textViewSubTitle.setTextColor(Color.parseColor("#808080"));
                    textViewSubTitle.setVisibility(View.VISIBLE);
                    editTextPhone.setVisibility(View.GONE);
                    editTextOtp.setVisibility(View.VISIBLE);
                    btnNext.setVisibility(View.GONE);
                    btnVerify.setVisibility(View.VISIBLE);
                    sendVerificationCode(phoneNumber);
                }else{
                    textViewSubTitle.setText("Mobile number is not valid");
                    textViewSubTitle.setTextColor(Color.parseColor("#FF0000"));
                    textViewSubTitle.setVisibility(View.VISIBLE);
                }
            }
        });

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Crash Testing", "Step 1");
                if (!editTextOtp.getText().toString().isEmpty() && editTextOtp.getText().toString().length() == 6){
                    Log.d("Crash Testing", "Step 2");
                    progressDialog.show();
                    verifyCode(editTextOtp.getText().toString());
                    Log.d("Crash Testing", "Step 3");
                }else{
                    textViewSubTitle.setText("OTP is not valid");
                    textViewSubTitle.setTextColor(Color.parseColor("#FF0000"));
                    textViewSubTitle.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        // inside this method we are checking if
        // the code entered is correct or not.
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // if the code is correct and the task is successful
                            String postUrl = GlobalData.BASE_URL+"user/add_user_number.php";
                            Map<String, String> postParams = new HashMap<>();
                            postParams.put("number", editTextPhone.getText().toString());
                            VolleyHelper.sendPostRequest(LoginActivity.this, postUrl, postParams, new VolleyHelper.VolleyCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    Log.d("POST Response", response);

                                    //Store login data into shared local storage
                                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean("isLoggedIn", true);
                                    editor.putString("number", editTextPhone.getText().toString());

                                    JSONObject jsonObject = null;

                                    try {
                                        jsonObject = new JSONObject(response);

                                        // Check status or message if needed
                                        String status = jsonObject.getString("status");

                                        // Get the nested "user" object
                                        JSONObject userObject = jsonObject.getJSONObject("user");
                                        String isProfileCompleted = userObject.getString("isProfileCompleted");


                                        editor.putString("firstName",userObject.getString("firstName"));
                                        editor.putString("lastName",userObject.getString("lastName"));
                                        editor.putString("email",userObject.getString("email"));
                                        editor.putString("userId",userObject.getString("userId"));
                                        editor.putString("isProfileCompleted", isProfileCompleted);
                                        editor.putString("isVolunteer", userObject.getString("isVolunteer"));
                                        editor.putString("organizationId", userObject.getString("organizationId"));
                                        editor.putString("isOrganization", userObject.getString("isOrganization"));
                                        editor.apply();

                                        String userId = userObject.getString("userId");
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("isAvailable", false); // default available
                                        if (userObject.getString("isVolunteer").equals("0")){
                                            userData.put("isVolunteer", false);
                                        }else {
                                            userData.put("isVolunteer", false);
                                        }

                                        FirebaseMessaging.getInstance().getToken()
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        String token = task.getResult();
                                                        userData.put("fcmTokens", token);
                                                         // Or your primary key
                                                        FirebaseDatabase.getInstance().getReference("user").child(String.valueOf(userId)).setValue(userData);
                                                    }
                                                });

                                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_LONG).show();

                                        if(isProfileCompleted.equals("0")){
                                            progressDialog.dismiss();
                                            Intent i = new Intent(LoginActivity.this, ProfileActivity.class);
                                            startActivity(i);
                                            finish();
                                        }else {
                                            progressDialog.dismiss();
                                            Intent i = new Intent(LoginActivity.this, HomeActivity.class);
                                            startActivity(i);
                                            finish();
                                        }
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("POST Error", error);
                                }
                            });


                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // (optional) Activity for callback binding
                        // If no activity is passed, reCAPTCHA verification can not be used.
                        .setCallbacks(mCallBack)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // callback method is called on Phone auth provider.
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks

            // initializing our callbacks for on
            // verification callback method.
            mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        // below method is used when
        // OTP is sent from Firebase
        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            // when we receive the OTP it
            // contains a unique id which
            // we are storing in our string
            // which we have already created.
            verificationId = s;
        }

        // this method is called when user
        // receive OTP from Firebase.
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            // below line is used for getting OTP code
            // which is sent in phone auth credentials.
            final String code = phoneAuthCredential.getSmsCode();

            // checking if the code
            // is null or not.
            if (code != null) {
                // if the code is not null then
                // we are setting that code to
                // our OTP edittext field.
                editTextOtp.setText(code);

                // after setting this code
                // to OTP edittext field we
                // are calling our verifycode method.
                verifyCode(code);
            }
        }

        // this method is called when firebase doesn't
        // sends our OTP code due to any error or issue.
        @Override
        public void onVerificationFailed(FirebaseException e) {
            // displaying error message with firebase exception.
            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    // below method is use to verify code from Firebase.
    private void verifyCode(String code) {
        // below line is used for getting
        // credentials from our verification id and code.
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);

        // after getting credential we are
        // calling sign in method.
        signInWithCredential(credential);
    }
}