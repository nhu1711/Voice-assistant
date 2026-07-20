package com.example.voiceassistant.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.services.VoiceAssistantService;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.utils.ServiceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.content.ComponentName;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchBackgroundMode;
    private SwitchMaterial switchBatteryAlert;
    private SwitchMaterial switchNetworkAlert;
    private SwitchMaterial switchReadNotifications;
    private LinearLayout layoutReadSettings;
    private RadioGroup rgReadMode;
    private MaterialButton btnSelectApps;
    private Slider sliderSpeed;
    private Slider sliderVolume;
    private MaterialButton btnLanguage;
    private SharedPreferences prefs;
    private TTSManager ttsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ttsManager = TTSManager.getInstance(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        initViews(view);
        loadPreferences();
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        switchBackgroundMode = view.findViewById(R.id.switch_background_mode);
        switchBatteryAlert = view.findViewById(R.id.switch_battery_warning);
        switchNetworkAlert = view.findViewById(R.id.switch_network_warning);
        switchReadNotifications = view.findViewById(R.id.switch_read_notifications);
        layoutReadSettings = view.findViewById(R.id.layout_read_settings);
        rgReadMode = view.findViewById(R.id.rg_read_mode);
        btnSelectApps = view.findViewById(R.id.btn_select_apps);
        sliderSpeed = view.findViewById(R.id.slider_tts_speed);
        sliderVolume = view.findViewById(R.id.slider_volume);
        btnLanguage = view.findViewById(R.id.btn_language);

        prefs = requireContext().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
    }

    private void loadPreferences() {
        boolean isServiceRunning = ServiceUtils.isServiceRunning(requireContext(), VoiceAssistantService.class);
        boolean backgroundPref = prefs.getBoolean(AppConstants.PREF_BACKGROUND_MODE, false);
        
        // Sync preference with actual service state
        if (backgroundPref != isServiceRunning) {
            prefs.edit().putBoolean(AppConstants.PREF_BACKGROUND_MODE, isServiceRunning).apply();
        }
        
        switchBackgroundMode.setChecked(isServiceRunning);
        switchBatteryAlert.setChecked(prefs.getBoolean(AppConstants.PREF_BATTERY_ALERT, true));
        switchNetworkAlert.setChecked(prefs.getBoolean(AppConstants.PREF_NETWORK_ALERT, true));
        
        boolean readNotifsEnabled = prefs.getBoolean(AppConstants.PREF_READ_NOTIFICATIONS, false);
        switchReadNotifications.setChecked(readNotifsEnabled);
        layoutReadSettings.setVisibility(readNotifsEnabled ? View.VISIBLE : View.GONE);
        
        int readMode = prefs.getInt(AppConstants.PREF_READ_MODE, AppConstants.READ_MODE_ANNOUNCE_ONLY);
        if (readMode == AppConstants.READ_MODE_AUTO) {
            rgReadMode.check(R.id.rb_read_auto);
        } else {
            rgReadMode.check(R.id.rb_read_announce);
        }
        
        sliderSpeed.setValue(prefs.getFloat(AppConstants.PREF_SPEECH_RATE, 1.0f));
        sliderVolume.setValue((float) prefs.getInt("volume_level", 80));

        updateLanguageButtonText();
    }

    private void setupListeners() {
        switchBackgroundMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(AppConstants.PREF_BACKGROUND_MODE, isChecked).apply();
            updateService(isChecked);
        });

        switchBatteryAlert.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefs.edit().putBoolean(AppConstants.PREF_BATTERY_ALERT, isChecked).apply());

        switchNetworkAlert.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefs.edit().putBoolean(AppConstants.PREF_NETWORK_ALERT, isChecked).apply());

        switchReadNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!isNotificationServiceEnabled()) {
                    // Prompt for permission
                    switchReadNotifications.setChecked(false);
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    Toast.makeText(requireContext(), "Please enable Notification Access for " + getString(R.string.app_name), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            prefs.edit().putBoolean(AppConstants.PREF_READ_NOTIFICATIONS, isChecked).apply();
            layoutReadSettings.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        
        rgReadMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = (checkedId == R.id.rb_read_auto) ? AppConstants.READ_MODE_AUTO : AppConstants.READ_MODE_ANNOUNCE_ONLY;
            prefs.edit().putInt(AppConstants.PREF_READ_MODE, mode).apply();
        });
        
        btnSelectApps.setOnClickListener(v -> {
            AppSelectionDialogFragment dialog = new AppSelectionDialogFragment();
            dialog.show(getChildFragmentManager(), "AppSelectionDialog");
        });

        sliderSpeed.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                prefs.edit().putFloat(AppConstants.PREF_SPEECH_RATE, value).apply();
                if (ttsManager != null) {
                    ttsManager.stop();
                    ttsManager.speak(getString(R.string.test_speech_rate));
                }
            }
        });

        sliderVolume.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                prefs.edit().putInt("volume_level", (int) value).apply();
                if (ttsManager != null) {
                    ttsManager.stop();
                    ttsManager.speak(getString(R.string.test_volume));
                }
            }
        });

        btnLanguage.setOnClickListener(v -> toggleLanguage());
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = requireContext().getPackageName();
        final String flat = Settings.Secure.getString(requireContext().getContentResolver(),
                "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && cn.getPackageName().equals(pkgName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateService(boolean start) {
        Context context = requireContext();
        Intent intent = new Intent(context, VoiceAssistantService.class);
        if (start) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } else {
            context.stopService(intent);
        }
    }

    private void toggleLanguage() {
        String currentLang = prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.LANGUAGE_ENGLISH);
        String newLang = currentLang.equals(AppConstants.LANGUAGE_VIETNAMESE) 
                ? AppConstants.LANGUAGE_ENGLISH 
                : AppConstants.LANGUAGE_VIETNAMESE;
        
        prefs.edit().putString(AppConstants.PREF_LANGUAGE, newLang).apply();
        updateLanguageButtonText();
        
        Toast.makeText(requireContext(), "Language changed to " + (newLang.equals("vi") ? "Tiếng Việt" : "English"), Toast.LENGTH_SHORT).show();
        
        // Refresh Activity to apply language
        requireActivity().recreate();
    }

    private void updateLanguageButtonText() {
        String currentLang = prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.LANGUAGE_ENGLISH);
        btnLanguage.setText(currentLang.equals(AppConstants.LANGUAGE_VIETNAMESE) ? R.string.vi_language : R.string.en_language);
    }
}
