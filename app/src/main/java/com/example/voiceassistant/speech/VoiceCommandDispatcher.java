package com.example.voiceassistant.speech;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.example.voiceassistant.R;
import com.example.voiceassistant.battery.BatteryManagerHelper;
import com.example.voiceassistant.call.CallManager;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.contacts.ContactManager;
import com.example.voiceassistant.repository.NotificationRepository;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.utils.TimeFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Locale;

public class VoiceCommandDispatcher {
    private static final String TAG = "VoiceCommandDispatcher";

    private final Activity activity;
    private final TTSManager ttsManager;
    private final ContactManager contactManager;
    private final CallManager callManager;
    private final BatteryManagerHelper batteryManagerHelper;
    private final CommandCallback callback;

    public interface CommandCallback {
        void onResponse(String text);
    }

    public interface EmergencyCommandCallback extends CommandCallback {
        void onSOSRequested();
    }

    public VoiceCommandDispatcher(Activity activity, TTSManager ttsManager, ContactManager contactManager,
                                  CallManager callManager, BatteryManagerHelper batteryManagerHelper,
                                  CommandCallback callback) {
        this.activity = activity;
        this.ttsManager = ttsManager;
        this.contactManager = contactManager;
        this.callManager = callManager;
        this.batteryManagerHelper = batteryManagerHelper;
        this.callback = callback;
    }

    public boolean execute(CommandParser.CommandResult result) {
        Log.d(TAG, "[VOICE] VOICE_INTENT: " + result.getIntent().name());
        ttsManager.setAssistantListening(false);
        
        switch (result.getIntent()) {
            case OPEN_OBJECT_DETECTION:
                processResponse(R.string.response_opening_camera);
                navigateTo(R.id.nav_object_detection);
                return true;
                
            case OPEN_EMERGENCY:
                if (callback instanceof EmergencyCommandCallback) {
                    ((EmergencyCommandCallback) callback).onSOSRequested();
                    return false;
                } else {
                    processResponse(R.string.response_opening_emergency);
                    navigateTo(R.id.nav_contacts);
                    return true;
                }
                
            case OPEN_SETTINGS:
                processResponse(R.string.response_opening_settings);
                navigateTo(R.id.nav_settings);
                return true;
                
            case GO_HOME:
                processResponse(R.string.response_returning_home);
                navigateTo(R.id.nav_home);
                return true;
                
            case CLOSE_APPLICATION:
                processResponse(R.string.response_closing_app);
                activity.finishAffinity();
                return true;
                
            case HELP:
                processResponse(R.string.response_help_guidance);
                return false;
                
            case REPEAT:
                ttsManager.repeatLastSpeech();
                return false;
                
            case CALL:
                handleCallCommand(result.getParam());
                return false;
                
            case TIME:
                handleTimeCommand();
                return false;
                
            case BATTERY:
                handleBatteryCommand();
                return false;
                
            case READ_NOTIFICATIONS:
                handleReadNotificationsCommand();
                return false;
                
            case STOP_OBJECT_DETECTION:
                processResponse(R.string.response_stopping_detection);
                navigateTo(R.id.nav_home);
                return true;
                
            case CANCEL:
                processResponse(R.string.response_canceled);
                return false;
                
            case UNKNOWN:
            default:
                processResponse(R.string.response_unknown_command);
                return false;
        }
    }

    private void processResponse(int stringResId) {
        String response = activity.getString(stringResId);
        if (callback != null) callback.onResponse(response);
        ttsManager.speakNow(response);
    }

    private void navigateTo(int navId) {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navId);
        }
    }
    
    private void handleCallCommand(String contactName) {
        ContactManager.ContactInfo contact = contactManager.findContactByName(contactName);
        if (contact != null) {
            String response = activity.getString(R.string.calling_contact, contact.getName());
            if (callback != null) callback.onResponse(response);
            ttsManager.speakNow(response);
            callManager.makeCall(contact.getPhoneNumber(), contact.getName());
        } else {
            String response = activity.getString(R.string.contact_not_found, contactName);
            if (callback != null) callback.onResponse(response);
            ttsManager.speakNow(response);
        }
    }

    private void handleTimeCommand() {
        try {
            String language = getCurrentLanguage();
            String displayText = TimeFormatter.getDisplayTime(activity);
            String speechText = TimeFormatter.getTimeSpeech(activity, language);
            if (callback != null) callback.onResponse(displayText);
            ttsManager.speakNow(speechText);
        } catch (Exception e) {
            String errorMsg = activity.getString(R.string.error_time);
            if (callback != null) callback.onResponse(errorMsg);
            ttsManager.speakNow(errorMsg);
        }
    }

    private void handleBatteryCommand() {
        String lang = getCurrentLanguage();
        String response = batteryManagerHelper.getBatterySpeechResponse(lang);
        String display = activity.getString(R.string.battery_status, batteryManagerHelper.getBatteryLevel());
        if (callback != null) callback.onResponse(display);
        ttsManager.speakNow(response);
    }

    private void handleReadNotificationsCommand() {
        List<NotificationRepository.NotificationItem> unread = NotificationRepository.getInstance().getUnreadNotifications();
        if (unread.isEmpty()) {
            String response = activity.getString(R.string.no_unread_notifications);
            if (callback != null) callback.onResponse(response);
            ttsManager.speakNow(response);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(activity.getString(R.string.reading_notifications, unread.size())).append(". ");
            for (NotificationRepository.NotificationItem item : unread) {
                sb.append(activity.getString(R.string.notif_read_full, item.getAppName(), item.getSender(), item.getContent())).append(". ");
            }
            if (callback != null) callback.onResponse(activity.getString(R.string.reading_notifications_ui, unread.size()));
            ttsManager.speakNow(sb.toString());
            NotificationRepository.getInstance().clearNotifications();
        }
    }

    private String getCurrentLanguage() {
        android.content.SharedPreferences prefs = activity.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        String systemLang = Locale.getDefault().getLanguage();
        String defaultLang = systemLang.equals("vi") ? AppConstants.LANGUAGE_VIETNAMESE : AppConstants.LANGUAGE_ENGLISH;
        return prefs.getString(AppConstants.PREF_LANGUAGE, defaultLang);
    }
}
