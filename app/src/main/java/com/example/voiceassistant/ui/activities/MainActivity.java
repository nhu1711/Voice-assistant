package com.example.voiceassistant.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private View fragmentContainer;
    private View permissionSetupContainer;
    private MaterialButton btnGrantRequiredPermissions;
    private MaterialButton btnOpenAppSettings;
    private TextView tvPermissionError;
    private ActivityResultLauncher<String[]> requiredPermissionsLauncher;
    private ActivityResultLauncher<Intent> appSettingsLauncher;
    private boolean hasRequestedRequiredPermissions;
    private boolean mainContentInitialized;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<<<<<<< HEAD
        setupPermissionLaunchers();
=======
        
        // Light Status Bar with dark icons
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.background));

>>>>>>> 57c73549ecdb92730ab75ec96b1bc0b5b3d00228
        setContentView(R.layout.activity_main);
        initViews();
        setupNavigation();
        setupPermissionActions();
        updatePermissionGate(savedInstanceState == null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permissionSetupContainer != null) {
            updatePermissionGate(false);
        }
    }

<<<<<<< HEAD
    private void setupPermissionLaunchers() {
        requiredPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> handleRequiredPermissionResult()
        );
        appSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updatePermissionGate(false)
        );
    }

    private void initViews() {
        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNav = findViewById(R.id.bottom_navigation);
        permissionSetupContainer = findViewById(R.id.permission_setup_container);
        btnGrantRequiredPermissions = findViewById(R.id.btn_grant_required_permissions);
        btnOpenAppSettings = findViewById(R.id.btn_open_app_settings);
        tvPermissionError = findViewById(R.id.tv_permission_error);
    }

    private void setupNavigation() {
=======
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

>>>>>>> 57c73549ecdb92730ab75ec96b1bc0b5b3d00228
        bottomNav.setOnItemSelectedListener(item -> {
            if (!PermissionHelper.hasRequiredSosPermissions(this)) {
                updatePermissionGate(false);
                return false;
            }

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
    }

    private void setupPermissionActions() {
        btnGrantRequiredPermissions.setOnClickListener(v -> requestMissingRequiredPermissions());
        btnOpenAppSettings.setOnClickListener(v -> openAppSettings());
    }

    private void updatePermissionGate(boolean shouldLoadDefaultFragment) {
        boolean hasRequiredPermissions = PermissionHelper.hasRequiredSosPermissions(this);
        permissionSetupContainer.setVisibility(hasRequiredPermissions ? View.GONE : View.VISIBLE);
        fragmentContainer.setVisibility(hasRequiredPermissions ? View.VISIBLE : View.GONE);
        bottomNav.setVisibility(hasRequiredPermissions ? View.VISIBLE : View.GONE);

        if (hasRequiredPermissions) {
            tvPermissionError.setVisibility(View.GONE);
            btnOpenAppSettings.setVisibility(View.GONE);
            if (shouldLoadDefaultFragment || !mainContentInitialized) {
                mainContentInitialized = true;
                loadDefaultFragmentIfNeeded();
            }
        }
    }

    private void loadDefaultFragmentIfNeeded() {
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void requestMissingRequiredPermissions() {
        List<String> missingPermissions = getMissingRequiredPermissions();
        if (missingPermissions.isEmpty()) {
            updatePermissionGate(false);
            return;
        }

        hasRequestedRequiredPermissions = true;
        requiredPermissionsLauncher.launch(missingPermissions.toArray(new String[0]));
    }

    private void handleRequiredPermissionResult() {
        if (PermissionHelper.hasRequiredSosPermissions(this)) {
            updatePermissionGate(false);
            return;
        }

        tvPermissionError.setText(R.string.permission_setup_denied_message);
        tvPermissionError.setVisibility(View.VISIBLE);
        btnOpenAppSettings.setVisibility(hasPermanentlyDeniedRequiredPermission() ? View.VISIBLE : View.GONE);
        updatePermissionGate(false);
        Toast.makeText(this, R.string.permission_setup_denied_message, Toast.LENGTH_LONG).show();
    }

    private List<String> getMissingRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        if (!PermissionHelper.hasCallPhonePermission(this)) {
            permissions.add(Manifest.permission.CALL_PHONE);
        }
        if (!PermissionHelper.hasSendSmsPermission(this)) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if (!PermissionHelper.hasLocationPermission(this)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!PermissionHelper.hasRecordAudioPermission(this)) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        return permissions;
    }

    private boolean hasPermanentlyDeniedRequiredPermission() {
        if (!hasRequestedRequiredPermissions) {
            return false;
        }

        for (String permission : getMissingRequiredPermissions()) {
            if (!shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        appSettingsLauncher.launch(intent);
    }
}
