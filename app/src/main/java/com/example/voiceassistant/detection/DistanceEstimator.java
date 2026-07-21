package com.example.voiceassistant.detection;

import android.graphics.RectF;

/**
 * Lớp ước lượng khoảng cách dựa trên kích thước của bounding box
 */
public class DistanceEstimator {

    public enum DistanceCategory {
        CLOSE,      // Xấp xỉ 1 mét
        MEDIUM,     // Xấp xỉ 2-3 mét
        FAR         // Ở xa
    }

    /**
     * Ước lượng nhóm khoảng cách dựa trên diện tích của bounding box
     * MediaPipe trả về tọa độ normalized [0.0, 1.0]
     * 
     * @param boundingBox Bounding box của vật thể
     * @return Nhóm khoảng cách tương ứng
     */
    public static DistanceCategory estimateDistanceCategory(RectF boundingBox) {
        if (boundingBox == null) {
            return DistanceCategory.FAR;
        }

        // Diện tích box chuẩn hóa (width * height)
        float area = boundingBox.width() * boundingBox.height();

        // Ngưỡng diện tích:
        // Hộp rất lớn (area > 0.15) -> CLOSE (~1 mét)
        // Hộp trung bình (area > 0.04) -> MEDIUM (~2-3 mét)
        // Hộp nhỏ -> FAR (ở xa)
        if (area > 0.15f) {
            return DistanceCategory.CLOSE;
        } else if (area > 0.04f) {
            return DistanceCategory.MEDIUM;
        } else {
            return DistanceCategory.FAR;
        }
    }

    /**
     * Lấy mô tả khoảng cách bằng tiếng Việt
     * 
     * @param category Nhóm khoảng cách
     * @return Chuỗi mô tả tiếng Việt
     */
    public static String getDistanceSpeech(DistanceCategory category) {
        switch (category) {
            case CLOSE:
                return "khoảng một mét";
            case MEDIUM:
                return "khoảng hai đến ba mét";
            case FAR:
            default:
                return "ở xa";
        }
    }
}
