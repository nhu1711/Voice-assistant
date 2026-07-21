package com.example.voiceassistant.battery;

import android.content.Context;
import android.os.BatteryManager;
import com.example.voiceassistant.R;

/**
 * Quản lý logic lấy thông tin pin (FR-06)
 */
public class BatteryManagerHelper {
    
    private final Context context;
    private final BatteryManager batteryManager;

    public BatteryManagerHelper(Context context) {
        this.context = context;
        this.batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }

    /**
     * Lấy phần trăm pin hiện tại
     */
    public int getBatteryLevel() {
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return 0;
    }

    /**
     * Kiểm tra xem thiết bị có đang sạc hay không
     */
    public boolean isCharging() {
        android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
        android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
            return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                   status == android.os.BatteryManager.BATTERY_STATUS_FULL;
        }
        return false;
    }

    /**
     * Tạo câu phản hồi TTS cho pin
     */
    public String getBatterySpeechResponse() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("voice_assistant_pref", android.content.Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "vi");
        return getBatterySpeechResponse(lang);
    }

    public String getBatterySpeechResponse(String language) {
        int level = getBatteryLevel();
        boolean charging = isCharging();
        
        String response = context.getString(R.string.battery_speech, level);
        
        if (charging) {
            response += context.getString(R.string.battery_charging);
        } else if (level < 20) {
            // Chỉ yêu cầu sạc khi pin thấp VÀ KHÔNG đang sạc
            response += context.getString(R.string.battery_low_warning);
        }
        
        return response;
    }
}
