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
    public static final String COMMAND_START_DETECT = "START_DETECT";
    public static final String COMMAND_STOP_DETECT = "STOP_DETECT";
    public static final String COMMAND_CLOSE_CAMERA = "CLOSE_CAMERA";
    public static final String COMMAND_REPEAT = "REPEAT";
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
    
    // ===== NOTIFICATION =====
    public static final String CHANNEL_ID = "voice_assistant_channel";
    public static final int NOTIFICATION_ID = 888;
    
    // ===== PREFERENCE KEYS =====
    public static final String PREF_BACKGROUND_MODE = "background_mode";
    public static final String PREF_BATTERY_ALERT = "battery_alert";
    public static final String PREF_NETWORK_ALERT = "network_alert";
    public static final String PREF_FIRST_RUN = "first_run";
    
    // ===== INTENT ACTIONS =====
    public static final String ACTION_STOP_SERVICE = "com.example.voiceassistant.STOP_SERVICE";
    
    // SOS
    public static final int SOS_CONFIRM_TIMEOUT = 5; // seconds
    public static final int MAX_EMERGENCY_CONTACTS = 5;
    
    // ===== NOTIFICATION READER =====
    public static final String COMMAND_READ_NOTIFICATIONS = "READ_NOTIFICATIONS";
    public static final String PREF_READ_NOTIFICATIONS = "read_notifications";
    public static final String PREF_READ_MODE = "read_mode";
    public static final String PREF_SELECTED_APPS = "selected_apps";
    public static final int READ_MODE_AUTO = 1;
    public static final int READ_MODE_ANNOUNCE_ONLY = 2;
}
