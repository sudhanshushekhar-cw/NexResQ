package com.example.nexresq;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Map;

public class VolleyHelper {

    public interface VolleyCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void sendPostRequest(Context context, String url, Map<String, String> params, VolleyCallback callback) {
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                callback::onSuccess,
                error -> callback.onError(error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(postRequest);
    }

    public static void sendGetRequest(Context context, String url, VolleyCallback callback) {
        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                callback::onSuccess,
                error -> callback.onError(error.toString())
        );
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(getRequest);
    }
}
