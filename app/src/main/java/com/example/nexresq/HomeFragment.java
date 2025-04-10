package com.example.nexresq;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        String postUrl = "http://192.168.1.7/nexresq/user/test.php";
        Map<String, String> postParams = new HashMap<>();
        postParams.put("username", "sudhanshu");
        postParams.put("age", "23");


        VolleyHelper.sendPostRequest(requireContext(), postUrl, postParams, new VolleyHelper.VolleyCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d("POST Response", response);
            }

            @Override
            public void onError(String error) {
                Log.e("POST Error", error);
            }
        });
        // Inflate the layout for this fragment
        return view;
    }
}