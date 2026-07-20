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
import com.example.voiceassistant.utils.ServiceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchBackgroundMode;
    private SwitchMaterial switchBatteryAlert;
    private SwitchMaterial switchNetworkAlert;
    private Slider sliderSpeed;
    private Slider sliderVolume;
    private MaterialButton btnLanguage;
    private SharedPreferences prefs;
    private com.example.voiceassistant.tts.TTSManager ttsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ttsManager = new com.example.voiceassistant.tts.TTSManager(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
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
