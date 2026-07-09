package com.example.voiceassistant.speech;

import com.example.voiceassistant.constants.AppConstants;

import java.util.regex.Pattern;

/**
 * Phân tích và xác định loại lệnh từ văn bản
 * UC-01 Step 8: Phân tích văn bản để xác định loại lệnh
 */
public class CommandParser {
    
    /**
     * Phân tích lệnh và trả về loại lệnh
     * 
     * @param text Văn bản cần phân tích
     * @return CommandResult chứa loại lệnh và tham số
     */
    public static CommandResult parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new CommandResult(AppConstants.COMMAND_UNKNOWN, text);
        }
        
        // Chuyển về chữ thường để so khớp chính xác
        String lowerText = text.toLowerCase().trim();
        
        // Kiểm tra lệnh CALL
        if (isCallCommand(lowerText)) {
            String contactName = extractContactName(lowerText);
            return new CommandResult(AppConstants.COMMAND_CALL, contactName);
        }
        
        // Kiểm tra lệnh TIME
        if (isTimeCommand(lowerText)) {
            return new CommandResult(AppConstants.COMMAND_TIME, text);
        }
        
        // Kiểm tra lệnh BATTERY
        if (isBatteryCommand(lowerText)) {
            return new CommandResult(AppConstants.COMMAND_BATTERY, text);
        }
        
        // Kiểm tra lệnh SOS
        if (isSOSCommand(lowerText)) {
            return new CommandResult(AppConstants.COMMAND_SOS, text);
        }
        
        // Kiểm tra lệnh DETECT
        if (isDetectCommand(lowerText)) {
            return new CommandResult(AppConstants.COMMAND_DETECT, text);
        }
        
        // Không xác định
        return new CommandResult(AppConstants.COMMAND_UNKNOWN, text);
    }
    
    /**
     * Kiểm tra lệnh gọi điện
     * Pattern: "gọi cho {tên}", "call {tên}"
     */
    private static boolean isCallCommand(String text) {
        // Hỗ trợ cả khi không có dấu hoặc viết hoa
        return text.contains("gọi cho") || text.contains("call") || 
               text.contains("goi cho") || text.contains("kêu");
    }
    
    /**
     * Trích xuất tên liên hệ từ lệnh gọi
     */
    private static String extractContactName(String text) {
        String[] keywords = {"gọi cho", "gọi điện cho", "gọi", "call", "goi cho", "goi", "kêu"};
        String result = text.toLowerCase(); // Làm việc trên bản chữ thường
        
        for (String keyword : keywords) {
            if (result.startsWith(keyword)) {
                result = result.substring(keyword.length()).trim();
                break;
            }
        }
        
        // Loại bỏ dấu chấm ở cuối nếu có
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        
        return result;
    }
    
    /**
     * Kiểm tra lệnh hỏi giờ
     */
    private static boolean isTimeCommand(String text) {
        String[] keywords = {"mấy giờ", "giờ rồi", "giờ", "mấy h", "time", 
                             "bây giờ", "hiện tại", "now", "what time"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra lệnh hỏi pin
     */
    private static boolean isBatteryCommand(String text) {
        String[] keywords = {"pin", "battery", "batter", "phần trăm", "còn bao nhiêu", 
                             "kiểm tra pin", "percent"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra lệnh SOS
     * FR-12: Các từ khóa kích hoạt SOS
     */
    private static boolean isSOSCommand(String text) {
        String[] keywords = {"cứu", "sos", "giúp", "help", "emergency", 
                             "khẩn cấp", "cấp cứu", "gọi khẩn", "cứu tôi", 
                             "cứu giúp", "tôi cần giúp"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra lệnh nhận diện vật thể
     * FR-03: Từ khóa mở camera detection
     */
    private static boolean isDetectCommand(String text) {
        return text.contains("nhận diện") || text.contains("detect") || 
               text.contains("vật thể") || text.contains("object");
    }
    
    /**
     * Kết quả phân tích lệnh
     */
    public static class CommandResult {
        private final String commandType;
        private final String commandText;
        
        public CommandResult(String commandType, String commandText) {
            this.commandType = commandType;
            this.commandText = commandText;
        }
        
        public String getCommandType() {
            return commandType;
        }
        
        public String getCommandText() {
            return commandText;
        }
    }
}
