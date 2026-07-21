package com.example.voiceassistant.ui.fragments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.voiceassistant.detection.MyCategory;
import com.example.voiceassistant.detection.MyDetection;
import com.example.voiceassistant.detection.LabelTranslator;
import com.example.voiceassistant.constants.AppConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom View vẽ khung nhận diện (bounding box) và tên vật thể lên khung hình camera
 */
public class DetectionOverlay extends View {

    private static final String TAG = "DetectionOverlay";
    private List<MyDetection> detections = new ArrayList<>();
    private int imageHeight = 0;
    private int imageWidth = 0;

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint textBackgroundPaint = new Paint();

    public DetectionOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        // Cấu hình vẽ viền hộp (xanh lá nổi bật)
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);

        // Cấu hình vẽ chữ nhãn
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setFakeBoldText(true);

        // Cấu hình vẽ nền cho chữ để dễ đọc trên mọi nền hình ảnh
        textBackgroundPaint.setColor(Color.BLACK);
        textBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Cập nhật kết quả nhận diện để vẽ lại
     * 
     * @param detections Danh sách vật thể phát hiện được
     * @param imageHeight Chiều cao của ảnh đầu vào (sau khi xoay)
     * @param imageWidth Chiều rộng của ảnh đầu vào (sau khi xoay)
     */
    public void setResults(List<MyDetection> detections, int imageHeight, int imageWidth) {
        this.detections = detections != null ? detections : new ArrayList<>();
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
        Log.d(TAG, "[Overlay] Overlay updated with " + this.detections.size() + " detections");
        invalidate(); // Yêu cầu vẽ lại view
    }

    /**
     * Xóa kết quả vẽ
     */
    public void clear() {
        this.detections.clear();
        this.imageHeight = 0;
        this.imageWidth = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);



        if (detections.isEmpty() || imageWidth == 0 || imageHeight == 0) {
            return;
        }

        // Tỷ lệ căn chỉnh tọa độ từ ảnh đầu vào sang kích thước của View trên màn hình
        // MediaPipe boundingBox trả về tọa độ pixel trên kích thước ảnh gốc
        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;

        for (MyDetection detection : detections) {
            RectF box = detection.boundingBox();

            // Tính toán tọa độ vẽ thực tế trên màn hình
            float left = box.left * scaleX;
            float top = box.top * scaleY;
            float right = box.right * scaleX;
            float bottom = box.bottom * scaleY;

            // Vẽ khung viền vật thể
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Lấy nhãn và điểm tin cậy
            if (detection.categories() != null && !detection.categories().isEmpty()) {
                MyCategory category = detection.categories().get(0);
                String enLabel = category.categoryName();
                
                String lang = getContext().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)
                        .getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
                String translatedLabel = LabelTranslator.translate(enLabel, lang);
                float score = category.score();

                Log.d(TAG, String.format("[Overlay] Bounding boxes drawn. Coordinates: [%.1f, %.1f, %.1f, %.1f] for %s", left, top, right, bottom, enLabel));

                String displayText = String.format("%s (%.0f%%)", translatedLabel, score * 100);

                // Đo chiều rộng của text để tạo hình chữ nhật nền vừa vặn
                float textWidth = textPaint.measureText(displayText);
                float textHeight = textPaint.getTextSize();

                // Vẽ nền đen cho nhãn chữ (đặt ở phía trên góc trái của hộp)
                canvas.drawRect(left, top - textHeight - 15, left + textWidth + 15, top, textBackgroundPaint);

                // Vẽ nhãn chữ màu trắng
                canvas.drawText(displayText, left + 8, top - 10, textPaint);
            }
        }
    }
}
