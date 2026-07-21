package com.example.voiceassistant.detection;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import com.example.voiceassistant.detection.LabelTranslator;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObjectDetectorManager {
    private static final String TAG = "ObjectDetectorManager";
    private static final String MODEL_PATH = "models/efficientdet_lite0.tflite";
    private static final int INPUT_SIZE = 320;
    private static final float SCORE_THRESHOLD = 0.35f;

    private Interpreter interpreter;
    private final Context context;
    private final DetectorListener listener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean isInitializing = false;
    private volatile boolean isClosed = false;
    private volatile boolean isDetecting = false;

    // COCO Labels map based on EfficientDet-Lite0
    private static final Map<Integer, String> COCO_LABELS = new HashMap<>();
    static {
        COCO_LABELS.put(0, "person"); COCO_LABELS.put(1, "bicycle"); COCO_LABELS.put(2, "car");
        COCO_LABELS.put(3, "motorcycle"); COCO_LABELS.put(4, "airplane"); COCO_LABELS.put(5, "bus");
        COCO_LABELS.put(6, "train"); COCO_LABELS.put(7, "truck"); COCO_LABELS.put(8, "boat");
        COCO_LABELS.put(9, "traffic light"); COCO_LABELS.put(10, "fire hydrant"); COCO_LABELS.put(12, "stop sign");
        COCO_LABELS.put(13, "parking meter"); COCO_LABELS.put(14, "bench"); COCO_LABELS.put(15, "bird");
        COCO_LABELS.put(16, "cat"); COCO_LABELS.put(17, "dog"); COCO_LABELS.put(18, "horse");
        COCO_LABELS.put(19, "sheep"); COCO_LABELS.put(20, "cow"); COCO_LABELS.put(21, "elephant");
        COCO_LABELS.put(22, "bear"); COCO_LABELS.put(23, "zebra"); COCO_LABELS.put(24, "giraffe");
        COCO_LABELS.put(26, "backpack"); COCO_LABELS.put(27, "umbrella"); COCO_LABELS.put(30, "handbag");
        COCO_LABELS.put(31, "tie"); COCO_LABELS.put(32, "suitcase"); COCO_LABELS.put(33, "frisbee");
        COCO_LABELS.put(34, "skis"); COCO_LABELS.put(35, "snowboard"); COCO_LABELS.put(36, "sports ball");
        COCO_LABELS.put(37, "kite"); COCO_LABELS.put(38, "baseball bat"); COCO_LABELS.put(39, "baseball glove");
        COCO_LABELS.put(40, "skateboard"); COCO_LABELS.put(41, "surfboard"); COCO_LABELS.put(42, "tennis racket");
        COCO_LABELS.put(43, "bottle"); COCO_LABELS.put(45, "wine glass"); COCO_LABELS.put(46, "cup");
        COCO_LABELS.put(47, "fork"); COCO_LABELS.put(48, "knife"); COCO_LABELS.put(49, "spoon");
        COCO_LABELS.put(50, "bowl"); COCO_LABELS.put(51, "banana"); COCO_LABELS.put(52, "apple");
        COCO_LABELS.put(53, "sandwich"); COCO_LABELS.put(54, "orange"); COCO_LABELS.put(55, "broccoli");
        COCO_LABELS.put(56, "carrot"); COCO_LABELS.put(57, "hot dog"); COCO_LABELS.put(58, "pizza");
        COCO_LABELS.put(59, "donut"); COCO_LABELS.put(60, "cake"); COCO_LABELS.put(61, "chair");
        COCO_LABELS.put(62, "couch"); COCO_LABELS.put(63, "potted plant"); COCO_LABELS.put(64, "bed");
        COCO_LABELS.put(66, "dining table"); COCO_LABELS.put(69, "toilet"); COCO_LABELS.put(71, "tv");
        COCO_LABELS.put(72, "laptop"); COCO_LABELS.put(73, "mouse"); COCO_LABELS.put(74, "remote");
        COCO_LABELS.put(75, "keyboard"); COCO_LABELS.put(76, "cell phone"); COCO_LABELS.put(77, "microwave");
        COCO_LABELS.put(78, "oven"); COCO_LABELS.put(79, "toaster"); COCO_LABELS.put(80, "sink");
        COCO_LABELS.put(81, "refrigerator"); COCO_LABELS.put(83, "book"); COCO_LABELS.put(84, "clock");
        COCO_LABELS.put(85, "vase"); COCO_LABELS.put(86, "scissors"); COCO_LABELS.put(87, "teddy bear");
        COCO_LABELS.put(88, "hair drier"); COCO_LABELS.put(89, "toothbrush");
    }

    public interface DetectorListener {
        void onResults(List<MyDetection> detections, int imageHeight, int imageWidth);
        void onError(String error);
    }

    public ObjectDetectorManager(Context context, DetectorListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initDetector();
    }

    private void initDetector() {
        if (interpreter != null || isInitializing) return;
        isInitializing = true;
        isClosed = false;

        executorService.execute(() -> {
            try {
                if (isClosed) return;
                Log.d(TAG, "[Detector] Loading model...");
                Log.d(TAG, "[Detector] Model path: " + MODEL_PATH);
                MappedByteBuffer modelBuffer = loadModelFile();
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(4);
                
                interpreter = new Interpreter(modelBuffer, options);
                Log.d(TAG, "[Detector] Model loaded successfully");
                
                // Tensor shapes validation logging
                int[] inputShape = interpreter.getInputTensor(0).shape();
                Log.d(TAG, "[Detector] Input tensor shape: [" + inputShape[0] + ", " + inputShape[1] + ", " + inputShape[2] + ", " + inputShape[3] + "], type: " + interpreter.getInputTensor(0).dataType());
                
                int outputTensorCount = interpreter.getOutputTensorCount();
                for (int i = 0; i < outputTensorCount; i++) {
                    Tensor tensor = interpreter.getOutputTensor(i);
                    int[] shape = tensor.shape();
                    StringBuilder shapeStr = new StringBuilder("[");
                    for (int s = 0; s < shape.length; s++) {
                        shapeStr.append(shape[s]).append(s < shape.length - 1 ? ", " : "");
                    }
                    shapeStr.append("]");
                    Log.d(TAG, "[Detector] Output tensor shape: " + shapeStr.toString() + ", dtype: " + tensor.dataType() + ", index: " + i);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "[Detector] Failed to initialize TFLite Interpreter. Exception: " + e.getMessage(), e);
                if (listener != null && !isClosed) {
                    listener.onError("Lỗi tải mô hình: " + e.getMessage());
                }
            } finally {
                isInitializing = false;
            }
        });
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void detectAsync(Bitmap image, long timestampMs, int rotationDegrees) {
        if (isClosed || isDetecting) {
            image.recycle();
            return;
        }

        if (interpreter == null) {
            initDetector();
            image.recycle();
            return;
        }

        isDetecting = true;
        executorService.execute(() -> {
            if (isClosed) {
                image.recycle();
                isDetecting = false;
                return;
            }
            try {
                Log.d(TAG, "[Detector] Inference started");
                long startTime = SystemClock.uptimeMillis();
                
                // Process image: rotate and resize to 320x320
                Bitmap rotatedBitmap = rotateBitmap(image, rotationDegrees);
                int originalWidth = rotatedBitmap.getWidth();
                int originalHeight = rotatedBitmap.getHeight();
                
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, INPUT_SIZE, INPUT_SIZE, true);
                
                ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);
                
                Log.d(TAG, "[Detector] Bitmap size: " + image.getWidth() + "x" + image.getHeight());
                Log.d(TAG, "[Detector] Rotation: " + rotationDegrees);
                Log.d(TAG, "[Detector] ByteBuffer capacity: " + inputBuffer.capacity());
                Log.d(TAG, "[Detector] Tensor input shape matches input size: " + INPUT_SIZE);
                
                // EfficientDet-Lite0 Outputs
                // 0: [1, 25, 4] Bounding boxes
                // 1: [1, 25] Classes
                // 2: [1, 25] Scores
                // 3: [1] Number of detections
                float[][][] outputBoxes = new float[1][25][4];
                float[][] outputClasses = new float[1][25];
                float[][] outputScores = new float[1][25];
                float[] outputCount = new float[1];
                
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, outputBoxes);
                outputs.put(1, outputClasses);
                outputs.put(2, outputScores);
                outputs.put(3, outputCount);
                
                Object[] inputs = {inputBuffer};
                
                interpreter.runForMultipleInputsOutputs(inputs, outputs);
                
                Log.d(TAG, "[Detector] Inference finished");
                
                List<MyDetection> detections = parseDetections(outputBoxes, outputClasses, outputScores, outputCount, originalWidth, originalHeight);
                
                if (listener != null && !isClosed) {
                    listener.onResults(detections, originalHeight, originalWidth);
                }
                
                if (resizedBitmap != rotatedBitmap) resizedBitmap.recycle();
                if (rotatedBitmap != image) rotatedBitmap.recycle();
                image.recycle();
                
            } catch (Exception e) {
                if (!isClosed) {
                    Log.e(TAG, "Error in detectAsync", e);
                    e.printStackTrace();
                }
            } finally {
                isDetecting = false;
            }
        });
    }
    
    private List<MyDetection> parseDetections(float[][][] outputBoxes, float[][] outputClasses, float[][] outputScores, float[] outputCount, int imgW, int imgH) {
        List<MyDetection> results = new ArrayList<>();
        int count = (int) outputCount[0];
        
        Log.d(TAG, "[Detector] outputCount: " + count);
        
        for (int i = 0; i < count; i++) {
            float score = outputScores[0][i];
            int classId = (int) outputClasses[0][i];
            String labelName = COCO_LABELS.getOrDefault(classId, "unknown");
            
            float ymin = outputBoxes[0][i][0];
            float xmin = outputBoxes[0][i][1];
            float ymax = outputBoxes[0][i][2];
            float xmax = outputBoxes[0][i][3];
            
            if (score < SCORE_THRESHOLD) {
                Log.d(TAG, "[Detector] Discarded detection index " + i + " WHY: confidence below threshold (" + score + " < " + SCORE_THRESHOLD + ")");
                continue;
            }
            
            if (labelName.equals("unknown")) {
                Log.d(TAG, "[Detector] Discarded detection index " + i + " WHY: unknown class id " + classId);
                continue;
            }
            
            if (xmin >= xmax || ymin >= ymax) {
                Log.d(TAG, "[Detector] Discarded detection index " + i + " WHY: invalid coordinates (negative width/height)");
                continue;
            }

            RectF box = new RectF(
                    Math.max(0, xmin * imgW),
                    Math.max(0, ymin * imgH),
                    Math.min(imgW, xmax * imgW),
                    Math.min(imgH, ymax * imgH)
            );
            
            String viLabel = LabelTranslator.translate(labelName);
            Log.d(TAG, String.format("[Detector] score: %.2f, class index: %d, bounding boxes: [%.1f, %.1f, %.1f, %.1f]", score, classId, box.left, box.top, box.right, box.bottom));
            Log.d(TAG, String.format("[Detector] COCO class id: %d, English label: %s, Vietnamese translation: %s", classId, labelName, viLabel));
            
            MyCategory category = new MyCategory(labelName, score);
            results.add(new MyDetection(box, category));
        }
        return results;
    }
    
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int val = intValues[pixel++];
                // TFLite uint8 model takes RGB as raw bytes without float normalization
                byteBuffer.put((byte) ((val >> 16) & 0xFF));
                byteBuffer.put((byte) ((val >> 8) & 0xFF));
                byteBuffer.put((byte) (val & 0xFF));
            }
        }
        return byteBuffer;
    }
    
    private Bitmap rotateBitmap(Bitmap source, int degrees) {
        if (degrees == 0) return source;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void close() {
        Log.d(TAG, "[Detector] close() called. Shutting down executor.");
        isClosed = true;
        
        executorService.shutdownNow(); // Ngắt lập tức các task đang chạy ngầm
        
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
            Log.d(TAG, "[Detector] Interpreter released");
        }
    }
}
