package com.example.nexresq;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Test extends AppCompatActivity {

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

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");
        Map<String, Object> testData = new HashMap<>();
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("status", "Firebase is working");

        ref.setValue(testData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("FIREBASE_TEST", "Write succeeded!");
            } else {
                Log.e("FIREBASE_TEST", "Write failed: " + task.getException());
            }
        });

    }
}