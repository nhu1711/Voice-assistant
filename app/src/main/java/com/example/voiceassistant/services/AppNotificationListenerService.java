package com.example.voiceassistant.services;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.repository.NotificationRepository;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.utils.LocaleHelper;

import java.util.Set;

public class AppNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "AppNotifListener";
    private TTSManager ttsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        ttsManager = TTSManager.getInstance(this);
        Log.d(TAG, "NotificationListenerService Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationListenerService Destroyed");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        
        boolean isEnabled = prefs.getBoolean(AppConstants.PREF_READ_NOTIFICATIONS, false);
        if (!isEnabled) return;

        Set<String> selectedApps = prefs.getStringSet(AppConstants.PREF_SELECTED_APPS, null);
        String packageName = sbn.getPackageName();

        if (selectedApps == null || !selectedApps.contains(packageName)) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        String title = extras.getString(Notification.EXTRA_TITLE, "");
        CharSequence textSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
        String content = textSeq != null ? textSeq.toString() : "";

        if (title.isEmpty() && content.isEmpty()) return;

        String appName = getAppName(packageName);
        
        // Save to repository
        NotificationRepository.getInstance().addNotification(appName, title, content);

        // Announce based on mode
        int mode = prefs.getInt(AppConstants.PREF_READ_MODE, AppConstants.READ_MODE_ANNOUNCE_ONLY);
        String lang = prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
        Context localizedContext = LocaleHelper.setLocale(this, lang);

        String speech;
        if (mode == AppConstants.READ_MODE_AUTO) {
            // "Bạn có tin nhắn Zalo mới từ A. Nội dung: ..."
            speech = localizedContext.getString(R.string.notif_read_full, appName, title, content);
        } else {
            // "Bạn có tin nhắn mới từ A"
            speech = localizedContext.getString(R.string.notif_announce_only, appName, title);
        }

        if (ttsManager != null && speech != null && !speech.isEmpty()) {
            ttsManager.speak(speech);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Do nothing for now
    }

    private String getAppName(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}
