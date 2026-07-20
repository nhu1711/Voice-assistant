package com.example.voiceassistant;

import android.app.Application;

import com.example.voiceassistant.tts.TTSManager;

public class VoiceAssistantApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize global components
        TTSManager.getInstance(this);
    }
}
