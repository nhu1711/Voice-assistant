package com.example.voiceassistant.constants;

public class AppConstants {
    // Permission Request Codes
    public static final int REQUEST_RECORD_AUDIO = 1001;
    public static final int REQUEST_CALL_PHONE = 1002;
    public static final int REQUEST_READ_CONTACTS = 1003;
    public static final int REQUEST_ACCESS_FINE_LOCATION = 1004;
    public static final int REQUEST_SEND_SMS = 1005;
    public static final int REQUEST_CAMERA = 1006;
    
    // Speech Recognition
    public static final String SPEECH_LANGUAGE_VI = "vi-VN";
    public static final String SPEECH_LANGUAGE_EN = "en-US";
    public static final int SPEECH_TIMEOUT = 5000; // milliseconds
    
    // TTS
    public static final float TTS_DEFAULT_SPEECH_RATE = 1.0f;
    public static final float TTS_DEFAULT_PITCH = 1.0f;
    
    // Command Types
    public static final String COMMAND_CALL = "CALL";
    public static final String COMMAND_TIME = "TIME";
    public static final String COMMAND_BATTERY = "BATTERY";
    public static final String COMMAND_SOS = "SOS";
    public static final String COMMAND_DETECT = "DETECT";
    public static final String COMMAND_UNKNOWN = "UNKNOWN";
    
    // Intent Actions
    public static final String ACTION_VOICE_COMMAND = "com.example.voiceassistant.VOICE_COMMAND";
    public static final String EXTRA_COMMAND_TEXT = "command_text";
    
    // SharedPreferences Keys
    public static final String PREF_NAME = "voice_assistant_pref";
    public static final String PREF_SPEECH_RATE = "speech_rate";
    public static final String PREF_LANGUAGE = "language";
    
    // ===== LANGUAGE =====
    public static final String LANGUAGE_VIETNAMESE = "vi";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String DEFAULT_LANGUAGE = LANGUAGE_VIETNAMESE;
    
    // ===== TIME FORMAT =====
    public static final String TIME_FORMAT_VI = "HH:mm";
    public static final String TIME_FORMAT_EN = "hh:mm a";
    
    // SOS
    public static final int SOS_CONFIRM_TIMEOUT = 5; // seconds
    public static final int MAX_EMERGENCY_CONTACTS = 5;
}
