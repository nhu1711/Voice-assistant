package com.example.voiceassistant.detection;

import java.util.HashMap;
import java.util.Map;

/**
 * Lớp tiện ích dịch nhãn từ tiếng Anh sang tiếng Việt
 */
public class LabelTranslator {

    private static final Map<String, String> translationMap = new HashMap<>();

    static {
        translationMap.put("person", "Người");
        translationMap.put("chair", "Ghế");
        translationMap.put("door", "Cửa");
        translationMap.put("car", "Ô tô");
        translationMap.put("bus", "Xe buýt");
        translationMap.put("truck", "Xe tải");
        translationMap.put("bottle", "Chai nước");
        translationMap.put("cup", "Cốc");
        translationMap.put("cell phone", "Điện thoại");
        translationMap.put("book", "Sách");
        translationMap.put("backpack", "Ba lô");
        translationMap.put("bicycle", "Xe đạp");
        translationMap.put("motorcycle", "Xe máy");
        translationMap.put("dog", "Chó");
        translationMap.put("cat", "Mèo");
        translationMap.put("traffic light", "Đèn giao thông");
        translationMap.put("stop sign", "Biển báo dừng");
        translationMap.put("dining table", "Bàn ăn");
        translationMap.put("laptop", "Máy tính xách tay");
        translationMap.put("keyboard", "Bàn phím");
        translationMap.put("mouse", "Chuột máy tính");
        translationMap.put("suitcase", "Va li");
        translationMap.put("umbrella", "Cái ô");
    }

    /**
     * Dịch nhãn tiếng Anh sang tiếng Việt dựa trên ngôn ngữ hiện tại
     * 
     * @param englishLabel Nhãn tiếng Anh
     * @param language Ngôn ngữ hiện tại ("vi" hoặc "en")
     * @return Nhãn tương ứng
     */
    public static String translate(String englishLabel, String language) {
        if (englishLabel == null || englishLabel.trim().isEmpty()) {
            return "";
        }
        
        if ("en".equals(language)) {
            // Viết hoa chữ cái đầu cho tiếng Anh
            return englishLabel.substring(0, 1).toUpperCase() + englishLabel.substring(1);
        }
        
        String key = englishLabel.toLowerCase().trim();
        if (translationMap.containsKey(key)) {
            return translationMap.get(key);
        }
        
        // Viết hoa chữ cái đầu nếu không tìm thấy bản dịch
        return englishLabel.substring(0, 1).toUpperCase() + englishLabel.substring(1);
    }
}
