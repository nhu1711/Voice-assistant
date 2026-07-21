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
        setContentView(R.layout.activity_main);

        // Yêu cầu tất cả các quyền cần thiết ngay khi vào ứng dụng
        PermissionHelper.requestAllPermissions(this);

        com.example.voiceassistant.tts.TTSManager ttsManager = com.example.voiceassistant.tts.TTSManager.getInstance(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
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
