package com.example.voiceassistant.speech;

import android.util.Log;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Phân tích và xác định loại lệnh từ văn bản bằng cách sử dụng Intents (Ý định)
 * UC-01 Step 8: Phân tích văn bản để xác định loại lệnh
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

    /**
     * Phân tích chuỗi và trả về Intent
     */
    public static CommandResult parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new CommandResult(VoiceIntent.UNKNOWN, text, "");
        }
        
        String normalized = normalizeText(text);
        Log.d(TAG, "[Parser] Original: \"" + text + "\"");
        Log.d(TAG, "[Parser] Normalized: \"" + normalized + "\"");

        // CALL
        if (containsAny(normalized, Arrays.asList("goi cho", "call", "goi dien cho", "keu"))) {
            String contactName = extractContactName(normalized);
            Log.d(TAG, "[Parser] Intent Detected: CALL, Param: " + contactName);
            return new CommandResult(VoiceIntent.CALL, text, contactName);
        }
        
        // OPEN_OBJECT_DETECTION
        if (containsAny(normalized, Arrays.asList(
                "mo nhan dien", "mo camera", "bat camera", "bat nhan dien", "nhan dien vat the",
                "quet vat the", "quet xung quanh", "xem phia truoc", "phia truoc co gi", 
                "nhin giup toi", "camera", "mo che do nhin", "object detect", "detect object",
                "object detector", "objects", "camera detection", "vision detection", 
                "recognition mode", "open object detection", "start object detection", 
                "detect objects", "recognize objects", "find objects", "scan objects", 
                "turn on camera", "start camera", "look around", "identify object", 
                "what is in front of me", "what do you see", "see surroundings", 
                "start vision", "object mode", "detect surroundings", "recognize surroundings", "look ahead"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: OPEN_OBJECT_DETECTION");
            return new CommandResult(VoiceIntent.OPEN_OBJECT_DETECTION, text, "");
        }

        // STOP_OBJECT_DETECTION
        if (containsAny(normalized, Arrays.asList(
                "tat camera", "dong camera", "thoat nhan dien", "dung nhan dien", "dung lai", "stop", "thoat",
                "stop object detection", "close object detection", "exit camera", 
                "stop camera", "turn camera off", "close vision", "cancel object detection", "stop recognition"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: STOP_OBJECT_DETECTION");
            return new CommandResult(VoiceIntent.STOP_OBJECT_DETECTION, text, "");
        }

        // GO_HOME
        if (containsAny(normalized, Arrays.asList(
                "ve trang chu", "tro ve", "quay lai", "ve home", "trang chu", "man hinh chinh",
                "go home", "home", "main page", "main screen", "return home", "back home", "return", "go back", "back", "previous"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: GO_HOME");
            return new CommandResult(VoiceIntent.GO_HOME, text, "");
        }

        // OPEN_EMERGENCY
        if (containsAny(normalized, Arrays.asList(
                "khan cap", "mo khan cap", "goi cuu ho", "sos", "cuu toi", "tro giup", "cuu", "giup", "cap cuu", "goi khan",
                "open emergency", "emergency", "help me", "call emergency", "panic"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: OPEN_EMERGENCY");
            return new CommandResult(VoiceIntent.OPEN_EMERGENCY, text, "");
        }

        // OPEN_SETTINGS
        if (containsAny(normalized, Arrays.asList(
                "mo cai dat", "cai dat", "thiet lap",
                "open settings", "settings", "preferences", "configuration"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: OPEN_SETTINGS");
            return new CommandResult(VoiceIntent.OPEN_SETTINGS, text, "");
        }

        // CLOSE_APPLICATION
        if (containsAny(normalized, Arrays.asList(
                "dong ung dung", "thoat ung dung", "thoat app",
                "close app", "exit", "quit", "close application"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: CLOSE_APPLICATION");
            return new CommandResult(VoiceIntent.CLOSE_APPLICATION, text, "");
        }

        // HELP
        if (containsAny(normalized, Arrays.asList(
                "huong dan", "lenh",
                "what can i say", "voice commands", "instructions", "show commands", "how do i use this"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: HELP");
            return new CommandResult(VoiceIntent.HELP, text, "");
        }

        // REPEAT
        if (containsAny(normalized, Arrays.asList(
                "nhac lai", "doc lai", "lap lai", "noi lai",
                "repeat", "say again", "repeat that", "speak again", "last message", "read again", "repeat last"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: REPEAT");
            return new CommandResult(VoiceIntent.REPEAT, text, "");
        }

        // CANCEL
        if (containsAny(normalized, Arrays.asList(
                "huy", "cancel", "never mind", "ignore", "stop listening"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: CANCEL");
            return new CommandResult(VoiceIntent.CANCEL, text, "");
        }
        
        // TIME
        if (containsAny(normalized, Arrays.asList(
                "may gio", "gio roi", "may h", "bay gio", "hien tai", "thoi gian", "bay h",
                "time", "what time", "current time", "now", "whats the time", "tell me the time"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: TIME");
            return new CommandResult(VoiceIntent.TIME, text, "");
        }

        // BATTERY
        if (containsAny(normalized, Arrays.asList(
                "pin", "phan tram pin", "con bao nhieu pin", "kiem tra pin", "muc pin", "dung luong pin",
                "battery", "percent", "how much battery", "battery level", "power left", "battery status", "check battery"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: BATTERY");
            return new CommandResult(VoiceIntent.BATTERY, text, "");
        }
        
        // READ_NOTIFICATIONS
        if (containsAny(normalized, Arrays.asList(
                "doc tin nhan", "doc thong bao", "co tin nhan", "tin nhan moi",
                "read message", "read messages", "read notification", "any messages"
        ))) {
            Log.d(TAG, "[Parser] Intent Detected: READ_NOTIFICATIONS");
            return new CommandResult(VoiceIntent.READ_NOTIFICATIONS, text, "");
        }

        Log.d(TAG, "[Parser] Intent Detected: UNKNOWN");
        return new CommandResult(VoiceIntent.UNKNOWN, text, "");
    }
    
    /**
     * Chuẩn hóa văn bản: xóa dấu tiếng Việt, chữ thường, bỏ ký tự đặc biệt
     */
    private static String normalizeText(String text) {
        if (text == null) return "";
        // Chuyển thành chữ thường
        String normalized = text.toLowerCase().trim();
        // Xóa dấu tiếng Việt
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalized = pattern.matcher(normalized).replaceAll("");
        // Chuyển đ thành d
        normalized = normalized.replaceAll("đ", "d");
        // Xóa ký tự đặc biệt (chỉ giữ lại a-z, 0-9 và khoảng trắng)
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");
        // Xóa khoảng trắng thừa
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    /**
     * Kiểm tra xem văn bản chuẩn hóa có chứa bất kỳ từ khóa nào không
     */
    private static boolean containsAny(String normalizedText, List<String> keywords) {
        for (String keyword : keywords) {
            // Kiểm tra match chính xác từ hoặc cụm từ
            if (normalizedText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private static String extractContactName(String normalizedText) {
        String[] keywords = {"goi cho", "goi dien cho", "goi", "call", "keu"};
        String result = normalizedText;
        
        for (String keyword : keywords) {
            if (result.startsWith(keyword)) {
                result = result.substring(keyword.length()).trim();
                break;
            }
        }
        return result;
    }
}
