package com.example.voiceassistant.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.example.voiceassistant.R;
import com.example.voiceassistant.battery.BatteryManagerHelper;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.tts.TTSManager;

/**
 * Lắng nghe các sự kiện hệ thống (FR-10, BR-09.2, BR-09.3)
 */
public class SystemBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemBroadcastReceiver";
    private static Boolean lastNetworkStatus = null; // null: unknown
    private static int lastBatteryAlertLevel = -1;

    private TTSManager ttsManager;

    public SystemBroadcastReceiver() {
    }

    public void setTtsManager(TTSManager ttsManager) {
        this.ttsManager = ttsManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        android.content.SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        
        // Chỉ xử lý nếu trợ lý nền đang bật
        if (!prefs.getBoolean(AppConstants.PREF_BACKGROUND_MODE, false)) return;

        String lang = prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
        Context localizedContext = com.example.voiceassistant.utils.LocaleHelper.setLocale(context, lang);

        if (ttsManager == null) {
            // Fallback in case not set
            ttsManager = new TTSManager(localizedContext);
        }

        switch (action) {
            case Intent.ACTION_BATTERY_CHANGED:
                handleBatteryThreshold(localizedContext, intent, prefs);
                break;
            case ConnectivityManager.CONNECTIVITY_ACTION:
                handleConnectivityChange(localizedContext, prefs);
                break;
        }
    }

    private void handleBatteryThreshold(Context context, Intent intent, android.content.SharedPreferences prefs) {
        if (prefs.getBoolean(AppConstants.PREF_BATTERY_ALERT, true)) {
            int level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) ((level / (float) scale) * 100);
            
            // BR-09.2: Chỉ cảnh báo khi xuống dưới 20% và chỉ đọc một lần duy nhất tại mỗi mức
            if (batteryPct <= 20 && batteryPct != lastBatteryAlertLevel) {
                BatteryManagerHelper batteryHelper = new BatteryManagerHelper(context);
                ttsManager.speak(batteryHelper.getBatterySpeechResponse());
                lastBatteryAlertLevel = batteryPct;
            }
        }
    }

    private void handleConnectivityChange(Context context, android.content.SharedPreferences prefs) {
        if (prefs.getBoolean(AppConstants.PREF_NETWORK_ALERT, true)) {
            boolean currentStatus = isNetworkAvailable(context);
            
            if (lastNetworkStatus != null) {
                // Cảnh báo khi trạng thái thay đổi
                if (lastNetworkStatus && !currentStatus) {
                    // Từ Có sang Không
                    ttsManager.speak(context.getString(R.string.internet_lost));
                    Log.d(TAG, "Internet Lost");
                } else if (!lastNetworkStatus && currentStatus) {
                    // Từ Không sang Có
                    ttsManager.speak(context.getString(R.string.internet_restored));
                    Log.d(TAG, "Internet Restored");
                }
            }
            lastNetworkStatus = currentStatus;
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        
        try {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) 
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } catch (SecurityException e) {
            Log.e(TAG, "Missing permission ACCESS_NETWORK_STATE", e);
            return true; // Assume available if can't check
        }
    }
}
