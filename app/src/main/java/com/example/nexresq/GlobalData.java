package com.example.nexresq;

import android.content.Context;
import android.content.SharedPreferences;

public class GlobalData {

        // Server URL
        // public static String BASE_URL = "https://coachingwood.in/nexresq/";
        public static String BASE_URL = "http://192.168.1.9/nexresq/";

        // Key Names
        public static final String PREFS_NAME = "MyPrefs";

        // Example to fetch userId if needed (dynamic)
        public static String getUserId(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("userId", "no user");  // default blank if not found
        }

        public static String getFirstName(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("firstName", "no user");  // default blank if not found
        }

        public static String getLastName(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("lastName", "no user");  // default blank if not found
        }

        public static String getOrganizationId(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("organizationId", "no user");  // default blank if not found
        }

        public static String isOrganization(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("isOrganization", "0");  // default blank if not found
        }

        public static String isVolunteer(Context context) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return sharedPreferences.getString("isVolunteer", "0");  // default blank if not found
        }
}
