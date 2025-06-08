package com.example.nexresq;

import  android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    View headerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        headerView =  navigationView.getHeaderView(0);

        Switch onDutySwitchButton = toolbar.findViewById(R.id.onDutySwitchButton);

        String isVolunteer = GlobalData.isVolunteer(HomeActivity.this);

        if(isVolunteer.equals("1")){
            onDutySwitchButton.setVisibility(View.VISIBLE);

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("user")
                    .child(GlobalData.getUserId(HomeActivity.this));

            // 🔁 Fetch current onDuty value from database
            ref.child("onDuty").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    Boolean onDuty = task.getResult().getValue(Boolean.class);
                    if (onDuty != null) {
                        onDutySwitchButton.setChecked(onDuty); // Set default state

                        // Set color and text based on state
                        if (onDuty) {
                            onDutySwitchButton.setTextColor(Color.parseColor("#4CAF50")); // Green
                            onDutySwitchButton.setText("On Duty");
                        } else {
                            onDutySwitchButton.setTextColor(Color.parseColor("#F44336")); // Red
                            onDutySwitchButton.setText("Off Duty");
                        }
                    }
                } else {
                    // Default to off if not found
                    onDutySwitchButton.setChecked(false);
                    onDutySwitchButton.setTextColor(Color.parseColor("#F44336")); // Red
                    onDutySwitchButton.setText("Off Duty");
                }
            });

            // 🔄 Listener to update Firebase when toggled
            onDutySwitchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Map<String, Object> userData = new HashMap<>();
                userData.put("onDuty", isChecked);
                ref.updateChildren(userData);

                if (isChecked) {
                    onDutySwitchButton.setTextColor(Color.parseColor("#4CAF50")); // Green
                    onDutySwitchButton.setText("On Duty");
                } else {
                    onDutySwitchButton.setTextColor(Color.parseColor("#F44336")); // Red
                    onDutySwitchButton.setText("Off Duty");
                }
            });

        } else {
            onDutySwitchButton.setVisibility(View.GONE);
        }




        Menu menu = navigationView.getMenu();
        MenuItem teamItem = menu.findItem(R.id.menuTeam);

        // Get the TextViews
        TextView userNameTextView = headerView.findViewById(R.id.userNameTextView);
        TextView userNumberTextView = headerView.findViewById(R.id.userNumberTextView);
        userNameTextView.setText(GlobalData.getFirstName(HomeActivity.this));
        userNumberTextView.setText(GlobalData.getUserId(HomeActivity.this));


        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String isOrganization = sharedPreferences.getString("isOrganization", "0");
        SharedPreferences.Editor editor = sharedPreferences.edit();


        if(isOrganization.equals("1")) {
            teamItem.setVisible(true);  // Show if organization
        } else {
            teamItem.setVisible(false); // Hide otherwise
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar, R.string.open_drawer, R.string.close_drawer );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

//        default Home fragment
        loadFragment(new HomeFragment(), 1);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                int menuHelpStatus = 1;
                int menuTeamStatus = 1;
                if(id == R.id.menuHome){
                    loadFragment(new HomeFragment(), 0);
                } else if (id == R.id.menuTeam) {
                    if(menuTeamStatus == 1){
                        loadFragment(new TeamFragment(), menuTeamStatus);
                        menuTeamStatus = 0;
                    }else{
                        loadFragment(new TeamFragment(), menuTeamStatus);
                    }
                } else if(id == R.id.menuHelp){
                    if(menuHelpStatus == 1){
                        loadFragment(new HelpFragment(), menuHelpStatus);
                        menuHelpStatus = 0;
                    }else{
                        loadFragment(new HelpFragment(), menuHelpStatus);
                    }
                } else if (id == R.id.menuSettings) {
                    Toast.makeText(HomeActivity.this, "Settings", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.menuLogout) {
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
                // Close the drawer after item is selected
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

    }

    private void loadFragment(Fragment fragment, int flag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (flag == 1)
            ft.add(R.id.container, fragment);
        else
            ft.replace(R.id.container, fragment);

        ft.commit();
    }
}