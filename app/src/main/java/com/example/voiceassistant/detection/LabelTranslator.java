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
        translationMap.put("bicycle", "Xe đạp");
        translationMap.put("car", "Ô tô");
        translationMap.put("motorcycle", "Xe máy");
        translationMap.put("airplane", "Máy bay");
        translationMap.put("bus", "Xe buýt");
        translationMap.put("train", "Tàu hỏa");
        translationMap.put("truck", "Xe tải");
        translationMap.put("boat", "Thuyền");
        translationMap.put("traffic light", "Đèn giao thông");
        translationMap.put("fire hydrant", "Trụ cứu hỏa");
        translationMap.put("stop sign", "Biển báo dừng");
        translationMap.put("parking meter", "Máy tính tiền đỗ xe");
        translationMap.put("bench", "Ghế đá");
        translationMap.put("bird", "Chim");
        translationMap.put("cat", "Mèo");
        translationMap.put("dog", "Chó");
        translationMap.put("horse", "Ngựa");
        translationMap.put("sheep", "Cừu");
        translationMap.put("cow", "Bò");
        translationMap.put("elephant", "Voi");
        translationMap.put("bear", "Gấu");
        translationMap.put("zebra", "Ngựa vằn");
        translationMap.put("giraffe", "Hươu cao cổ");
        translationMap.put("backpack", "Ba lô");
        translationMap.put("umbrella", "Cái ô");
        translationMap.put("handbag", "Túi xách");
        translationMap.put("tie", "Cà vạt");
        translationMap.put("suitcase", "Va li");
        translationMap.put("frisbee", "Đĩa ném");
        translationMap.put("skis", "Ván trượt tuyết");
        translationMap.put("snowboard", "Ván trượt tuyết");
        translationMap.put("sports ball", "Bóng thể thao");
        translationMap.put("kite", "Diều");
        translationMap.put("baseball bat", "Gậy bóng chày");
        translationMap.put("baseball glove", "Găng tay bóng chày");
        translationMap.put("skateboard", "Ván trượt");
        translationMap.put("surfboard", "Ván lướt sóng");
        translationMap.put("tennis racket", "Vợt tennis");
        translationMap.put("bottle", "Chai nước");
        translationMap.put("wine glass", "Ly rượu");
        translationMap.put("cup", "Cốc");
        translationMap.put("fork", "Cái nĩa");
        translationMap.put("knife", "Con dao");
        translationMap.put("spoon", "Cái thìa");
        translationMap.put("bowl", "Cái bát");
        translationMap.put("banana", "Quả chuối");
        translationMap.put("apple", "Quả táo");
        translationMap.put("sandwich", "Bánh mì kẹp");
        translationMap.put("orange", "Quả cam");
        translationMap.put("broccoli", "Bông cải xanh");
        translationMap.put("carrot", "Cà rốt");
        translationMap.put("hot dog", "Xúc xích");
        translationMap.put("pizza", "Bánh pizza");
        translationMap.put("donut", "Bánh donut");
        translationMap.put("cake", "Bánh ngọt");
        translationMap.put("chair", "Ghế");
        translationMap.put("couch", "Ghế sofa");
        translationMap.put("potted plant", "Chậu cây");
        translationMap.put("bed", "Giường");
        translationMap.put("dining table", "Bàn ăn");
        translationMap.put("toilet", "Bồn cầu");
        translationMap.put("tv", "Tivi");
        translationMap.put("laptop", "Máy tính xách tay");
        translationMap.put("mouse", "Chuột máy tính");
        translationMap.put("remote", "Điều khiển từ xa");
        translationMap.put("keyboard", "Bàn phím");
        translationMap.put("cell phone", "Điện thoại");
        translationMap.put("microwave", "Lò vi sóng");
        translationMap.put("oven", "Lò nướng");
        translationMap.put("toaster", "Máy nướng bánh mì");
        translationMap.put("sink", "Bồn rửa");
        translationMap.put("refrigerator", "Tủ lạnh");
        translationMap.put("book", "Sách");
        translationMap.put("clock", "Đồng hồ");
        translationMap.put("vase", "Lọ hoa");
        translationMap.put("scissors", "Cái kéo");
        translationMap.put("teddy bear", "Gấu bông");
        translationMap.put("hair drier", "Máy sấy tóc");
        translationMap.put("toothbrush", "Bàn chải đánh răng");
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
        
        return "Vật thể chưa xác định";
    }
}
