package com.example.voiceassistant.speech;

import android.util.Log;
import com.example.voiceassistant.constants.AppConstants;
import java.util.Arrays;
import java.util.List;

/**
 * Phân tích và xác định loại lệnh từ văn bản (Sửa đổi theo logic chuẩn của người dùng)
 */
public class CommandParser {
    
    private static final String TAG = "CommandParser";

    public static class CommandResult {
        private final VoiceIntent intent;
        private final String originalText;
        private final String param;
        
        public CommandResult(VoiceIntent intent, String originalText, String param) {
            this.intent = intent;
            this.originalText = originalText;
            this.param = param;
        }
        
        public VoiceIntent getIntent() { return intent; }
        public String getOriginalText() { return originalText; }
        public String getParam() { return param; }
    }

    public static CommandResult parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new CommandResult(VoiceIntent.UNKNOWN, text, "");
        }
        
        // Chuyển về chữ thường nhưng GIỮ NGUYÊN DẤU để trích xuất tên chính xác
        String lowerText = text.toLowerCase().trim();
        Log.d(TAG, "[Parser] Input: \"" + text + "\"");

        // 1. Kiểm tra lệnh CALL (Ưu tiên hàng đầu)
        if (isCallCommand(lowerText)) {
            String contactName = extractContactName(lowerText);
            Log.d(TAG, "[Parser] Intent: CALL, Name: " + contactName);
            return new CommandResult(VoiceIntent.CALL, text, contactName);
        }
        
        // 2. Kiểm tra lệnh NHẬN DIỆN VẬT THỂ
        if (isDetectCommand(lowerText)) {
            Log.d(TAG, "[Parser] Intent: OPEN_OBJECT_DETECTION");
            return new CommandResult(VoiceIntent.OPEN_OBJECT_DETECTION, text, "");
        }

        // 3. Kiểm tra lệnh DỪNG NHẬN DIỆN
        if (isStopDetectCommand(lowerText)) {
            Log.d(TAG, "[Parser] Intent: STOP_OBJECT_DETECTION");
            return new CommandResult(VoiceIntent.STOP_OBJECT_DETECTION, text, "");
        }

        // 4. Kiểm tra lệnh THỜI GIAN
        if (isTimeCommand(lowerText)) {
            Log.d(TAG, "[Parser] Intent: TIME");
            return new CommandResult(VoiceIntent.TIME, text, "");
        }

        // 5. Kiểm tra lệnh PIN
        if (isBatteryCommand(lowerText)) {
            Log.d(TAG, "[Parser] Intent: BATTERY");
            return new CommandResult(VoiceIntent.BATTERY, text, "");
        }

        // 6. Kiểm tra lệnh SOS
        if (isSOSCommand(lowerText)) {
            Log.d(TAG, "[Parser] Intent: OPEN_EMERGENCY");
            return new CommandResult(VoiceIntent.OPEN_EMERGENCY, text, "");
        }
        
        // 7. Kiểm tra lệnh ĐỌC THÔNG BÁO
        if (isReadNotificationsCommand(lowerText)) {
            Log.d(TAG, "[Parser] Intent: READ_NOTIFICATIONS");
            return new CommandResult(VoiceIntent.READ_NOTIFICATIONS, text, "");
        }

        // 8. Lệnh QUAY VỀ / TRANG CHỦ
        if (containsAny(lowerText, Arrays.asList("về trang chủ", "trở về", "quay lại", "home", "back"))) {
            return new CommandResult(VoiceIntent.GO_HOME, text, "");
        }

        // 9. Lệnh NHẮC LẠI
        if (containsAny(lowerText, Arrays.asList("nhắc lại", "đọc lại", "lặp lại", "nói lại", "repeat"))) {
            return new CommandResult(VoiceIntent.REPEAT, text, "");
        }

        Log.d(TAG, "[Parser] Intent: UNKNOWN");
        return new CommandResult(VoiceIntent.UNKNOWN, text, "");
    }

    private static boolean isCallCommand(String text) {
        return text.contains("gọi cho") || text.contains("call") || 
               text.contains("goi cho") || text.contains("kêu");
    }

    private static String extractContactName(String text) {
        String[] keywords = {"gọi cho", "gọi điện cho", "gọi", "call", "goi cho", "goi", "kêu"};
        String result = text;
        
        for (String keyword : keywords) {
            if (result.startsWith(keyword)) {
                result = result.substring(keyword.length()).trim();
                break;
            }
        }
        
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }

    private static boolean isTimeCommand(String text) {
        String[] keywords = {"mấy giờ", "giờ rồi", "mấy h", "bây giờ", "hiện tại", "thời gian", "time", "what time"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private static boolean isBatteryCommand(String text) {
        String[] keywords = {"pin", "phần trăm pin", "battery", "percent", "battery level"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private static boolean isSOSCommand(String text) {
        String[] keywords = {"cứu", "sos", "giúp", "help", "emergency", "khẩn cấp"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private static boolean isDetectCommand(String text) {
        return text.contains("nhận diện") || text.contains("detect") || 
               text.contains("vật thể") || text.contains("object") || text.contains("mở camera");
    }

    private static boolean isStopDetectCommand(String text) {
        return text.contains("tắt camera") || text.contains("đóng camera") || 
               text.contains("thoát nhận diện") || text.contains("dừng nhận diện");
    }

    private static boolean isReadNotificationsCommand(String text) {
        String[] keywords = {"đọc tin nhắn", "đọc thông báo", "tin nhắn mới", "read message", "read notification"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
