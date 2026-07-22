package com.example.voiceassistant.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;

public class SplashActivity extends AppCompatActivity {
    //khai bao : Quản lý Text-to-Speech, dùng để đọc chào mừng
    private com.example.voiceassistant.tts.TTSManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Khởi tạo TTS và chào mừng
        ttsManager = com.example.voiceassistant.tts.TTSManager.getInstance(this);
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
            boolean isFirstRun = prefs.getBoolean(AppConstants.PREF_FIRST_RUN, true);

            if (!isFirstRun) {
                // Chỉ chào mừng nếu không phải lần đầu (lần đầu sẽ có Onboarding)
                ttsManager.speakNow(getString(R.string.welcome_back));
            }

            if (isFirstRun) {
                startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
            finish();
        }, 2000); // 2 seconds splash
    }
}
