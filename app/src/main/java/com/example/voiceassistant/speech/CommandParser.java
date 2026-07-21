package com.example.voiceassistant.speech;

import android.util.Log;
import com.example.voiceassistant.constants.AppConstants;
import java.text.Normalizer;
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
        String normalized = removeVietnameseDiacritics(lowerText);
        Log.d(TAG, "[Parser] Input: \"" + text + "\"");

        // 1. Kiểm tra lệnh SOS
        if (isSOSCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: OPEN_EMERGENCY");
            return new CommandResult(VoiceIntent.OPEN_EMERGENCY, text, "");
        }

        // 2. Kiểm tra lệnh CALL
        if (isCallCommand(lowerText, normalized)) {
            String contactName = extractContactName(lowerText);
            Log.d(TAG, "[Parser] Intent: CALL, Name: " + contactName);
            return new CommandResult(VoiceIntent.CALL, text, contactName);
        }

        // 3. Kiểm tra lệnh DỪNG NHẬN DIỆN
        if (isStopDetectCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: STOP_OBJECT_DETECTION");
            return new CommandResult(VoiceIntent.STOP_OBJECT_DETECTION, text, "");
        }
        
        // 4. Kiểm tra lệnh NHẬN DIỆN VẬT THỂ
        if (isDetectCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: OPEN_OBJECT_DETECTION");
            return new CommandResult(VoiceIntent.OPEN_OBJECT_DETECTION, text, "");
        }

        // 5. Kiểm tra lệnh THỜI GIAN
        if (isTimeCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: TIME");
            return new CommandResult(VoiceIntent.TIME, text, "");
        }

        // 6. Kiểm tra lệnh PIN
        if (isBatteryCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: BATTERY");
            return new CommandResult(VoiceIntent.BATTERY, text, "");
        }

        // 7. Kiểm tra lệnh CÀI ĐẶT
        if (isSettingsCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: OPEN_SETTINGS");
            return new CommandResult(VoiceIntent.OPEN_SETTINGS, text, "");
        }
        
        // 8. Kiểm tra lệnh ĐỌC THÔNG BÁO
        if (isReadNotificationsCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: READ_NOTIFICATIONS");
            return new CommandResult(VoiceIntent.READ_NOTIFICATIONS, text, "");
        }

        // 9. Lệnh QUAY VỀ / TRANG CHỦ
        if (isGoHomeCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: GO_HOME");
            return new CommandResult(VoiceIntent.GO_HOME, text, "");
        }

        // 10. Lệnh ĐÓNG ỨNG DỤNG
        if (isCloseApplicationCommand(normalized)) {
            Log.d(TAG, "[Parser] Intent: CLOSE_APPLICATION");
            return new CommandResult(VoiceIntent.CLOSE_APPLICATION, text, "");
        }

        // 11. Lệnh HƯỚNG DẪN
        if (isHelpCommand(normalized)) {
            Log.d(TAG, "[Parser] Intent: HELP");
            return new CommandResult(VoiceIntent.HELP, text, "");
        }

        // 12. Lệnh NHẮC LẠI
        if (isRepeatCommand(lowerText, normalized)) {
            Log.d(TAG, "[Parser] Intent: REPEAT");
            return new CommandResult(VoiceIntent.REPEAT, text, "");
        }

        // 13. Lệnh HỦY
        if (isCancelCommand(normalized)) {
            Log.d(TAG, "[Parser] Intent: CANCEL");
            return new CommandResult(VoiceIntent.CANCEL, text, "");
        }

        Log.d(TAG, "[Parser] Intent: UNKNOWN");
        return new CommandResult(VoiceIntent.UNKNOWN, text, "");
    }

    private static boolean isCallCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("gọi cho", "gọi điện cho", "call"))
                || containsAny(normalized, Arrays.asList("goi cho", "goi dien cho", "keu"));
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

    private static boolean isTimeCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("mấy giờ", "giờ rồi", "mấy h", "bây giờ", "hiện tại", "thời gian"))
                || containsAny(normalized, Arrays.asList(
                "may gio", "gio roi", "doc gio", "may h", "bay gio", "hien tai", "thoi gian", "bay h",
                "time", "what time", "current time", "now", "whats the time", "tell me the time"
        ));
    }

    private static boolean isBatteryCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("pin", "phần trăm pin", "battery", "percent", "battery level"))
                || containsAny(normalized, Arrays.asList("pin", "phan tram pin", "battery", "percent", "battery level"));
    }

    private static boolean isSOSCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("cứu", "sos", "giúp", "help", "emergency", "khẩn cấp"))
                || containsAny(normalized, Arrays.asList(
                "cuu", "cuu toi", "goi khan cap", "kich hoat sos", "sos", "goi cuu ho",
                "toi can giup do", "cap cuu", "mo khan cap", "khan cap",
                "open emergency", "emergency", "help", "help me", "call emergency", "panic"
        ));
    }

    private static boolean isSettingsCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("cài đặt", "thiết lập", "settings", "setting"))
                || containsAny(normalized, Arrays.asList(
                "mo cai dat", "cai dat", "thiet lap", "open settings", "settings", "preferences", "configuration"
        ));
    }

    private static boolean isDetectCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("nhận diện", "vật thể", "mở camera", "detect", "object"))
                || containsAny(normalized, Arrays.asList("nhan dien", "vat the", "mo camera", "detect", "object"));
    }

    private static boolean isStopDetectCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("tắt camera", "đóng camera", "thoát nhận diện", "dừng nhận diện"))
                || containsAny(normalized, Arrays.asList("tat camera", "dong camera", "thoat nhan dien", "dung nhan dien"));
    }

    private static boolean isReadNotificationsCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("đọc tin nhắn", "đọc thông báo", "tin nhắn mới", "read message", "read notification"))
                || containsAny(normalized, Arrays.asList("doc tin nhan", "doc thong bao", "tin nhan moi", "read message", "read notification"));
    }

    private static boolean isGoHomeCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("về trang chủ", "trở về", "quay lại", "home", "back"))
                || containsAny(normalized, Arrays.asList(
                "ve trang chu", "tro ve", "quay lai", "ve home", "trang chu", "man hinh chinh",
                "go home", "home", "main page", "main screen", "return home", "back home", "return", "go back", "back", "previous"
        ));
    }

    private static boolean isCloseApplicationCommand(String normalized) {
        return containsAny(normalized, Arrays.asList(
                "dong ung dung", "thoat ung dung", "thoat app", "close app", "exit", "quit", "close application"
        ));
    }

    private static boolean isHelpCommand(String normalized) {
        return containsAny(normalized, Arrays.asList(
                "huong dan", "lenh", "what can i say", "voice commands", "instructions", "show commands", "how do i use this"
        ));
    }

    private static boolean isRepeatCommand(String text, String normalized) {
        return containsAny(text, Arrays.asList("nhắc lại", "đọc lại", "lặp lại", "nói lại", "repeat"))
                || containsAny(normalized, Arrays.asList(
                "nhac lai", "doc lai", "lap lai", "noi lai", "repeat", "say again", "repeat that",
                "speak again", "last message", "read again", "repeat last"
        ));
    }

    private static boolean isCancelCommand(String normalized) {
        return containsAny(normalized, Arrays.asList("huy", "cancel", "never mind", "ignore", "stop listening"));
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private static String removeVietnameseDiacritics(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd');
    }
}
