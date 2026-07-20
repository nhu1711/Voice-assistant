package com.example.voiceassistant.services;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.repository.NotificationRepository;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.utils.LocaleHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AppNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "AppNotifListener";
    private TTSManager ttsManager;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingTasks = new HashMap<>();
    private final Map<String, String> lastReadContent = new HashMap<>();
    private static final long DEBOUNCE_DELAY = 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        ttsManager = TTSManager.getInstance(this);
        Log.d(TAG, "NotificationListenerService Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        debounceHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "NotificationListenerService Destroyed");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.isOngoing()) return;

        final String notificationKey = sbn.getKey();
        final String packageName = sbn.getPackageName();

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(AppConstants.PREF_READ_NOTIFICATIONS, false);
        if (!isEnabled) return;

        Set<String> selectedApps = prefs.getStringSet(AppConstants.PREF_SELECTED_APPS, null);
        if (selectedApps == null || !selectedApps.contains(packageName)) return;

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        String title = extras.getString(Notification.EXTRA_TITLE, "");
        CharSequence textSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
        String content = textSeq != null ? textSeq.toString() : "";

        if (title.isEmpty() && content.isEmpty()) return;

        String combinedContent = title + "|" + content;
        if (combinedContent.equals(lastReadContent.get(notificationKey))) {
            return;
        }

        Runnable existingTask = pendingTasks.get(notificationKey);
        if (existingTask != null) {
            debounceHandler.removeCallbacks(existingTask);
        }

        Runnable readTask = () -> {
            processNotification(packageName, title, content, notificationKey, combinedContent);
            pendingTasks.remove(notificationKey);
        };

        pendingTasks.put(notificationKey, readTask);
        debounceHandler.postDelayed(readTask, DEBOUNCE_DELAY);
    }

    private void processNotification(String packageName, String title, String content, String key, String combined) {
        String appName = getAppName(packageName);
        NotificationRepository.getInstance().addNotification(appName, title, content);

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        int mode = prefs.getInt(AppConstants.PREF_READ_MODE, AppConstants.READ_MODE_ANNOUNCE_ONLY);
        String lang = prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
        Context localizedContext = LocaleHelper.setLocale(this, lang);

        String finalTitle = title != null ? title : "";
        if (!finalTitle.isEmpty() && appName != null) {
            String lowerTitle = finalTitle.toLowerCase();
            String lowerAppName = appName.toLowerCase();
            if (lowerTitle.startsWith(lowerAppName)) {
                if (lowerTitle.equals(lowerAppName)) {
                    finalTitle = "";
                } else {
                    finalTitle = finalTitle.substring(appName.length()).trim();
                    if (finalTitle.startsWith(":") || finalTitle.startsWith("-")) {
                        finalTitle = finalTitle.substring(1).trim();
                    }
                }
            }
        }

        String finalContent = content != null ? content : "";
        if (!finalTitle.isEmpty() && !finalContent.isEmpty()) {
            if (finalContent.toLowerCase().startsWith(finalTitle.toLowerCase())) {
                finalContent = finalContent.substring(finalTitle.length()).trim();
                if (finalContent.startsWith(":") || finalContent.startsWith("-")) {
                    finalContent = finalContent.substring(1).trim();
                }
            }
        }

        String speech;
        if (mode == AppConstants.READ_MODE_AUTO) {
            if (finalTitle.isEmpty()) {
                speech = localizedContext.getString(R.string.notif_read_full, appName, "", finalContent)
                        .replace(", .", ".").replace(" ,", ",").trim();
            } else {
                speech = localizedContext.getString(R.string.notif_read_full, appName, finalTitle, finalContent);
            }
        } else {
            if (finalTitle.isEmpty()) {
                speech = localizedContext.getString(R.string.notif_announce_only, appName, "")
                        .replace(", ", "").trim();
            } else {
                speech = localizedContext.getString(R.string.notif_announce_only, appName, finalTitle);
            }
        }

        if (ttsManager != null && speech != null && !speech.isEmpty()) {
            ttsManager.speak(speech);
            lastReadContent.put(key, combined);
        }
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
