package com.example.voiceassistant.ui.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.voiceassistant.R;
import com.example.voiceassistant.ui.fragments.ObjectDetectionFragment;
import com.example.voiceassistant.ui.fragments.ContactsFragment;
import com.example.voiceassistant.ui.fragments.HomeFragment;
import com.example.voiceassistant.ui.fragments.SettingsFragment;
import com.example.voiceassistant.permissions.PermissionHelper;
import com.example.voiceassistant.utils.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Light Status Bar with dark icons
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.background));

        setContentView(R.layout.activity_main);

        // Yêu cầu tất cả các quyền cần thiết ngay khi vào ứng dụng
        PermissionHelper.requestAllPermissions(this);

        com.example.voiceassistant.tts.TTSManager ttsManager = com.example.voiceassistant.tts.TTSManager.getInstance(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        // Custom BottomNav styling
        int primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary);
        int accentColor = androidx.core.content.ContextCompat.getColor(this, R.color.accent);
        
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_selected },
            new int[] { -android.R.attr.state_selected }
        };
        int[] colors = new int[] { accentColor, primaryColor };
        android.content.res.ColorStateList colorStateList = new android.content.res.ColorStateList(states, colors);
        
        bottomNav.setItemIconTintList(colorStateList);
        bottomNav.setItemTextColor(colorStateList);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            String pageName = "";

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                pageName = getString(R.string.nav_home);
            } else if (itemId == R.id.nav_contacts) {
                selectedFragment = new ContactsFragment();
                pageName = getString(R.string.nav_contacts);
            } else if (itemId == R.id.nav_object_detection) {
                selectedFragment = new ObjectDetectionFragment();
                pageName = getString(R.string.nav_object_detection);
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
                pageName = getString(R.string.nav_settings);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                
                // Voice feedback for navigation
                ttsManager.speakNow(pageName);
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }
}
