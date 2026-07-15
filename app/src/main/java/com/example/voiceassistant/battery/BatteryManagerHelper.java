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
     * Tạo câu phản hồi TTS cho pin
     */
    public String getBatterySpeechResponse() {
        int level = getBatteryLevel();
        String response = context.getString(R.string.battery_speech, level);
        
        // Cảnh báo nếu pin dưới 20%
        if (level < 20) {
            response += context.getString(R.string.battery_low_warning);
        }
        
        return response;
    }
}
