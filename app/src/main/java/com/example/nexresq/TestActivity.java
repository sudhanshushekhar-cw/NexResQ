package com.example.nexresq;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        String postUrl = "http://192.168.1.7/nexresq/user/post.php";
        Map<String, String> postParams = new HashMap<>();
        postParams.put("username", "sudhanshu");
        postParams.put("age", "23");


        VolleyHelper.sendPostRequest(getApplicationContext(), postUrl, postParams, new VolleyHelper.VolleyCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d("POST Response", response);
            }

            @Override
            public void onError(String error) {
                Log.e("POST Error", error);
            }
        });

        String getUrl = "http://192.168.1.7/nexresq/user/test.php?data=123";

//        VolleyHelper.sendGetRequest(getApplicationContext(), getUrl, new VolleyHelper.VolleyCallback() {
//            @Override
//            public void onSuccess(String response) {
//                Log.d("GET Response", response);
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e("GET Error", error);
//            }
//        });

    }
}