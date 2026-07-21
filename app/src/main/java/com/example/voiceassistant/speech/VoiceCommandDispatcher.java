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

    /**
     * Executes the command and returns true if the command resulted in navigation
     * or closing the app, meaning the current Fragment might be destroyed or hidden.
     */
    public boolean execute(CommandParser.CommandResult result) {
        Log.d(TAG, "[VOICE] VOICE_INTENT: " + result.getIntent().name());
        ttsManager.setAssistantListening(false); // Explicitly unblock TTS queue
        
        switch (result.getIntent()) {
            case OPEN_OBJECT_DETECTION:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Opening Object Detection");
                speakTranslated("Opening camera.", "Đang mở camera.");
                navigateTo(R.id.nav_object_detection);
                return true;
                
            case OPEN_EMERGENCY:
                if (callback instanceof EmergencyCommandCallback) {
                    Log.d(TAG, "[VOICE] VOICE_ACTION: Starting SOS");
                    ((EmergencyCommandCallback) callback).onSOSRequested();
                    return false;
                } else {
                    Log.d(TAG, "[VOICE] VOICE_ACTION: Opening Emergency");
                    speakTranslated("Opening emergency screen.", "Đang mở màn hình khẩn cấp.");
                    navigateTo(R.id.nav_contacts);
                    return true;
                }
                
            case OPEN_SETTINGS:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Opening Settings");
                speakTranslated("Opening settings.", "Đang mở cài đặt.");
                navigateTo(R.id.nav_settings);
                return true;
                
            case GO_HOME:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Returning Home");
                speakTranslated("Returning to the home screen.", "Trở về màn hình chính.");
                navigateTo(R.id.nav_home);
                return true;
                
            case CLOSE_APPLICATION:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Closing Application");
                speakTranslated("Closing application.", "Đang đóng ứng dụng.");
                activity.finishAffinity();
                return true;
                
            case HELP:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Help");
                speakTranslated("You can say: Open object detection, Open emergency, Open settings, Go home.", 
                               "Bạn có thể nói: Mở camera, Mở khẩn cấp, Mở cài đặt, hoặc Trở về trang chủ.");
                return false;
                
            case REPEAT:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Repeat");
                ttsManager.repeatLastSpeech();
                return false;
                
            case CALL:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Call");
                handleCallCommand(result.getParam());
                return false;
                
            case TIME:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Time");
                handleTimeCommand();
                return false;
                
            case BATTERY:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Battery");
                handleBatteryCommand();
                return false;
                
            case READ_NOTIFICATIONS:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Read Notifications");
                handleReadNotificationsCommand();
                return false;
                
            case STOP_OBJECT_DETECTION:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Stop Detection");
                // Mặc định dừng nhận diện nếu đang ở CameraFragment sẽ được xử lý riêng
                // Hoặc có thể hiểu là đóng camera -> về trang chủ
                speakTranslated("Stopping object detection and returning home.", "Đã dừng nhận diện vật thể và quay về màn hình chính.");
                navigateTo(R.id.nav_home);
                return true;
                
            case CANCEL:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Cancel");
                speakTranslated("Canceled.", "Đã hủy lệnh.");
                return false;
                
            case UNKNOWN:
            default:
                Log.d(TAG, "[VOICE] VOICE_ACTION: Unknown");
                speakTranslated("I'm sorry. I didn't understand that command. Please try again.", 
                               "Xin lỗi, tôi không hiểu lệnh đó. Vui lòng thử lại.");
                return false;
        }
    }

    private void navigateTo(int navId) {
        Log.d(TAG, "[VOICE] VOICE_NAVIGATION to menu item: " + navId);
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navId);
        }
    }
    
    private void speakTranslated(String enText, String viText) {
        String lang = getCurrentLanguage();
        String text = lang.equals(AppConstants.LANGUAGE_VIETNAMESE) ? viText : enText;
        Log.d(TAG, "[VOICE] VOICE_TTS: " + text);
        if (callback != null) callback.onResponse(text);
        ttsManager.speakNow(text);
    }
    
    private void handleCallCommand(String contactName) {
        Log.d(TAG, "Handling CALL command for: " + contactName);
        ContactManager.ContactInfo contact = contactManager.findContactByName(contactName);

        if (contact != null) {
            Log.d(TAG, "Contact found: " + contact.getName() + " - " + contact.getPhoneNumber());
            String response = activity.getString(R.string.calling_contact, contact.getName());
            if (callback != null) callback.onResponse(response);
            ttsManager.speakNow(response);
            callManager.makeCall(contact.getPhoneNumber(), contact.getName());
        } else {
            Log.w(TAG, "Contact NOT found in system: " + contactName);
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
            Log.d(TAG, "[VOICE] VOICE_TTS: " + speechText);
        } catch (Exception e) {
            Log.e(TAG, "Error handling time command", e);
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
        Log.d(TAG, "[VOICE] VOICE_TTS: " + response);
    }

    private void handleReadNotificationsCommand() {
        List<NotificationRepository.NotificationItem> unread =
                NotificationRepository.getInstance().getUnreadNotifications();

        if (unread.isEmpty()) {
            String response = activity.getString(R.string.no_unread_notifications);
            if (callback != null) callback.onResponse(response);
            ttsManager.speakNow(response);
            Log.d(TAG, "[VOICE] VOICE_TTS: " + response);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(activity.getString(R.string.reading_notifications, unread.size())).append(". ");
            for (NotificationRepository.NotificationItem item : unread) {
                sb.append(activity.getString(R.string.notif_read_full, item.getAppName(), item.getSender(), item.getContent())).append(". ");
            }
            if (callback != null) callback.onResponse(activity.getString(R.string.reading_notifications_ui, unread.size()));
            ttsManager.speakNow(sb.toString());
            Log.d(TAG, "[VOICE] VOICE_TTS: " + sb.toString());

            // Clear after reading
            NotificationRepository.getInstance().clearNotifications();
        }
    }

    private String getCurrentLanguage() {
        android.content.SharedPreferences prefs = activity.getSharedPreferences(
                AppConstants.PREF_NAME, Context.MODE_PRIVATE
        );
        String systemLang = Locale.getDefault().getLanguage();
        String defaultLang = systemLang.equals("vi") ? AppConstants.LANGUAGE_VIETNAMESE : AppConstants.LANGUAGE_ENGLISH;

        return prefs.getString(AppConstants.PREF_LANGUAGE, defaultLang);
    }
}
