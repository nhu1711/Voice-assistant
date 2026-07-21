package com.example.voiceassistant.ui.fragments;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.voiceassistant.R;
import com.example.voiceassistant.battery.BatteryManagerHelper;
import com.example.voiceassistant.call.CallManager;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.contacts.ContactManager;
import com.example.voiceassistant.detection.DistanceEstimator;
import com.example.voiceassistant.detection.LabelTranslator;
import com.example.voiceassistant.detection.ObjectDetectorManager;
import com.example.voiceassistant.permissions.PermissionHelper;
import com.example.voiceassistant.speech.CommandParser;
import com.example.voiceassistant.speech.SpeechRecognizerManager;
import com.example.voiceassistant.speech.VoiceCommandDispatcher;
import com.example.voiceassistant.speech.VoiceIntent;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.ui.activities.MainActivity;
import com.example.voiceassistant.utils.TimeFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.example.voiceassistant.detection.MyCategory;
import com.example.voiceassistant.detection.MyDetection;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment hiển thị camera và thực hiện nhận diện vật thể thời gian thực (Sub-flow 4)
 */
public class ObjectDetectionFragment extends Fragment implements ObjectDetectorManager.DetectorListener {

    private static final String TAG = "ObjectDetectionFragment";
    private static final long SPEECH_COOLDOWN_MS = 5000; // Cooldown phát thanh 5 giây

    private PreviewView viewFinder;
    private DetectionOverlay detectionOverlay;
    private TextView tvDetectionResult;
    private MaterialButton btnMicro;

    private ObjectDetectorManager detectorManager;
    private TTSManager ttsManager;
    private SpeechRecognizerManager speechRecognizerManager;
    
    // Thêm các manager khác để hỗ trợ gọi điện/xem giờ ngay trên camera screen
    private ContactManager contactManager;
    private CallManager callManager;
    private BatteryManagerHelper batteryManagerHelper;

    private ExecutorService cameraExecutor;
    private Bitmap bitmapBuffer = null;

    public enum DetectorState {
        IDLE,
        RUNNING,
        PAUSED_BY_VOICE,
        STOPPED
    }
    private DetectorState detectorState = DetectorState.RUNNING;
    
    private VoiceCommandDispatcher voiceCommandDispatcher;
    private ObjectAnimator pulseAnimator;
    private ProcessCameraProvider cameraProvider;

    // Đệm tái sử dụng để xử lý row stride padding và đếm khung hình
    private ByteBuffer cleanBuffer = null;
    private byte[] rowData = null;
    private int frameCount = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Các biến lưu thông tin phát âm gần nhất để kiểm soát cooldown
    private String lastSpokenObjectName = "";
    private String lastSpokenDirection = "";
    private DistanceEstimator.DistanceCategory lastSpokenDistanceCategory = null;
    private String lastSpokenText = "";
    private long lastSpeechTime = 0;
    private long lastTimestamp = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_object_detection, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        cameraExecutor = Executors.newSingleThreadExecutor();
        initManagers();
        setupListeners();



        // Kiểm tra và yêu cầu quyền camera
        if (PermissionHelper.hasCameraPermission(requireContext())) {
            startCamera();
        } else {
            PermissionHelper.requestCameraPermission(requireActivity());
        }
    }

    private void initViews(View view) {
        viewFinder = view.findViewById(R.id.view_finder);
        detectionOverlay = view.findViewById(R.id.detection_overlay);
        tvDetectionResult = view.findViewById(R.id.tv_detection_result);
        btnMicro = view.findViewById(R.id.btn_camera_micro);
    }

    private void initManagers() {
        Context context = requireContext();
        
        detectorManager = new ObjectDetectorManager(context, this);
        
        ttsManager = TTSManager.getInstance(context);
        ttsManager.setListener(new TTSManager.TTSListener() {
            @Override
            public void onSpeechStarted() {}

            @Override
            public void onSpeechFinished() {}

            @Override
            public void onSpeechError(String error) {
                Log.e(TAG, "TTS Error: " + error);
            }
        });

        // Setup Pulse Animation
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                btnMicro,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f)
        );
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

        initSpeechRecognizer();
        
        contactManager = new ContactManager(context);
        callManager = new CallManager(context, ttsManager);
        batteryManagerHelper = new BatteryManagerHelper(context);

        voiceCommandDispatcher = new VoiceCommandDispatcher(requireActivity(), ttsManager, contactManager, callManager, batteryManagerHelper, new VoiceCommandDispatcher.CommandCallback() {
            @Override
            public void onResponse(String text) {
                tvDetectionResult.setText(text);
            }
        });
    }

    private void initSpeechRecognizer() {
        speechRecognizerManager = new SpeechRecognizerManager(requireContext(), new SpeechRecognizerManager.RecognitionCallback() {
            @Override
            public void onReadyForSpeech() {
                Log.d(TAG, "[VOICE] Ready");
                mainHandler.post(() -> {
                    if (!isAdded() || isDetached() || getView() == null) return;
                    tvDetectionResult.setText(R.string.status_listening);
                    startPulseAnimation();
                });
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "[VOICE] Begin");
                mainHandler.post(() -> {
                    if (!isAdded() || isDetached() || getView() == null) return;
                    detectorState = DetectorState.PAUSED_BY_VOICE;
                    Log.d(TAG, "[Detector] Paused");
                    ttsManager.setAssistantListening(true);
                });
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "[VOICE] End");
                mainHandler.post(() -> {
                    if (!isAdded() || isDetached() || getView() == null) return;
                    stopPulseAnimation();
                    ttsManager.setAssistantListening(false);
                });
            }

            @Override
            public void onResult(String recognizedText) {
                Log.d(TAG, "[VOICE] Result: " + recognizedText);
                mainHandler.post(() -> {
                    if (!isAdded() || isDetached() || getView() == null) return;
                    stopPulseAnimation();
                    processFragmentVoiceCommand(recognizedText);
                    resumeDetector();
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "[VOICE] Error: " + errorMessage);
                mainHandler.post(() -> {
                    if (!isAdded() || isDetached() || getView() == null) return;
                    stopPulseAnimation();
                    
                    String currentText = tvDetectionResult.getText().toString();
                    String listeningText = getString(R.string.status_listening);
                    if (!currentText.isEmpty() && !currentText.equals(listeningText) && currentText.length() > 5) {
                        processFragmentVoiceCommand(currentText);
                        resumeDetector();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.error_general) + ": " + errorMessage, Toast.LENGTH_SHORT).show();
                        resumeDetector();
                    }
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                mainHandler.post(() -> {
                    if (!isAdded() || isDetached() || getView() == null) return;
                    tvDetectionResult.setText(partialText);
                });
            }
        });
    }

    private void resumeDetector() {
        if (detectorState == DetectorState.PAUSED_BY_VOICE) {
            detectorState = DetectorState.RUNNING;
            Log.d(TAG, "[VOICE] VOICE_RESUME_DETECTOR");
            Log.d(TAG, "[Detector] Resumed");
        }
        if (detectorState == DetectorState.RUNNING) {
            tvDetectionResult.setText(R.string.detection_status_running);
        } else {
            tvDetectionResult.setText(R.string.detection_status_stopped);
        }
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null && !pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            btnMicro.setScaleX(1f);
            btnMicro.setScaleY(1f);
        }
    }

    private void setupListeners() {
        btnMicro.setOnClickListener(v -> {
            Log.d(TAG, "[VOICE] VOICE_BUTTON_CLICKED");
            if (!PermissionHelper.hasRecordAudioPermission(requireContext())) {
                PermissionHelper.requestRecordAudioPermission(requireActivity());
                return;
            }
            
            if (speechRecognizerManager.isListening()) {
                stopVoiceRecognition();
            } else {
                startVoiceRecognition();
            }
        });
    }

    private void startVoiceRecognition() {
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(getContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "[VOICE] VOICE_START_LISTENING");
        detectorState = DetectorState.PAUSED_BY_VOICE;
        Log.d(TAG, "[Detector] Paused");
        ttsManager.stop();
        speechRecognizerManager.startListening();
    }

    private void stopVoiceRecognition() {
        speechRecognizerManager.stopListening();
        stopPulseAnimation();
        resumeDetector();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "[Camera] Use case binding failed", e);
                Toast.makeText(getContext(), "Không thể khởi động camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        
        Log.d(TAG, "[Camera] Preview started");

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        // Loại bỏ ép kiểu RGBA_8888 để tránh lỗi trên thiết bị không hỗ trợ. 
        // CameraX mặc định sẽ trả về YUV_420_888 và được xử lý tự động bởi hàm toBitmap()
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // FPS tracking
        final long[] lastFpsTime = {SystemClock.uptimeMillis()};
        final int[] frameCounter = {0};

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (frameCount == 0) {
                Log.d(TAG, "[Camera] Analyzer started");
            }
            frameCount++;
            frameCounter[0]++;
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            int format = imageProxy.getFormat(); // Mặc định thường là 35 (YUV_420_888)

            long currentFpsTime = SystemClock.uptimeMillis();
            if (currentFpsTime - lastFpsTime[0] >= 1000) {
                float fps = frameCounter[0] * 1000f / (currentFpsTime - lastFpsTime[0]);
                Log.d(TAG, "[Camera] Frames per second: " + fps);
                frameCounter[0] = 0;
                lastFpsTime[0] = currentFpsTime;
            }

            if (frameCount % 30 == 0) { // Log every 30 frames to avoid spamming
                Log.d(TAG, "[Camera] Frame received: count = " + frameCount);
                Log.d(TAG, "[Camera] Rotation: " + rotation);
                Log.d(TAG, "[Camera] Format: " + format);
            }

            if (detectorState != DetectorState.RUNNING) {
                if (frameCount % 30 == 0) {
                    Log.d(TAG, "[Detector] Frame ignored because detector paused");
                }
                imageProxy.close();
                return;
            }

            try {
                // Tự động convert YUV (hoặc bất kỳ format nào được hỗ trợ) sang RGB Bitmap
                Bitmap frameBitmap = imageProxy.toBitmap();
                
                if (frameBitmap == null) {
                    Log.e(TAG, "Failed to convert image to Bitmap");
                    return;
                }

                long timestamp = SystemClock.uptimeMillis();
                if (timestamp <= lastTimestamp) {
                    timestamp = lastTimestamp + 1;
                }
                lastTimestamp = timestamp;

                if (frameCount % 30 == 0) {
                    Log.d(TAG, "[Camera] Timestamp: " + timestamp);
                    Log.d(TAG, "[Camera] detectAsync called");
                }
                
                detectorManager.detectAsync(frameBitmap, timestamp, rotation);

                // Bitmap frameBitmap được sử dụng bởi luồng nền bên trong ObjectDetectorManager.
                // Nó sẽ tự động được recycle() bên trong manager sau khi xử lý xong.
            } catch (Exception e) {
                Log.e(TAG, "Error in image analysis analyzer at frame " + frameCount, e);
            } finally {
                imageProxy.close();
            }
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind camera to lifecycle", e);
        }
    }

    public void onResults(List<MyDetection> detections, int imageHeight, int imageWidth) {
        int objectsCount = detections != null ? detections.size() : 0;
        Log.d(TAG, "[Camera] listener.onResults() called");
        Log.d(TAG, "[Camera] number of detections: " + objectsCount);
        
        mainHandler.post(() -> {
            if (!isAdded() || isDetached() || getView() == null) return;

            // Truyền kết quả hiển thị cho overlay vẽ khung viền
            detectionOverlay.setResults(detections, imageHeight, imageWidth);
            Log.d(TAG, "Overlay updated with " + objectsCount + " bounding boxes.");

            if (detections == null || detections.isEmpty()) {
                tvDetectionResult.setText(R.string.detection_no_object);
                return;
            }

            // Chọn vật thể có kích thước (diện tích hộp) lớn nhất để ưu tiên thông báo
            MyDetection prominentDetection = null;
            float maxArea = 0;

            for (MyDetection d : detections) {
                RectF box = d.boundingBox();
                float area = box.width() * box.height();
                if (area > maxArea) {
                    maxArea = area;
                    prominentDetection = d;
                }
            }

            if (prominentDetection != null && prominentDetection.categories() != null && !prominentDetection.categories().isEmpty()) {
                MyCategory category = prominentDetection.categories().get(0);
                String enLabel = category.categoryName();
                String lang = getCurrentLanguage();
                String translatedLabel = LabelTranslator.translate(enLabel, lang);
                float score = category.score();

                // Tính toán hướng (trái, phải, phía trước) dựa trên tâm X của vật thể
                RectF box = prominentDetection.boundingBox();
                float centerXNormalized = (box.left + box.right) / 2.0f / imageWidth;

                String direction;
                if (centerXNormalized < 0.35f) {
                    direction = getString(R.string.direction_left);
                } else if (centerXNormalized > 0.65f) {
                    direction = getString(R.string.direction_right);
                } else {
                    direction = getString(R.string.direction_front);
                }

                // Tính toán diện tích tương đối để ước lượng khoảng cách
                float areaNormalized = (box.width() * box.height()) / (float) (imageWidth * imageHeight);
                DistanceEstimator.DistanceCategory distCategory;
                if (areaNormalized > 0.15f) {
                    distCategory = DistanceEstimator.DistanceCategory.CLOSE;
                } else if (areaNormalized > 0.04f) {
                    distCategory = DistanceEstimator.DistanceCategory.MEDIUM;
                } else {
                    distCategory = DistanceEstimator.DistanceCategory.FAR;
                }

                String distanceText = DistanceEstimator.getDistanceSpeech(requireContext(), distCategory);
                
                // Hiển thị kết quả văn bản
                String displayResult = getString(R.string.detection_pattern, translatedLabel, direction, distanceText);
                tvDetectionResult.setText(displayResult + String.format(" (%.0f%%)", score * 100));

                // Kiểm soát cooldown chống spam phát thanh
                long currentTime = System.currentTimeMillis();
                long timeSinceLastSpeech = currentTime - lastSpeechTime;

                boolean isNewObject = !translatedLabel.equals(lastSpokenObjectName) 
                        || !direction.equals(lastSpokenDirection) 
                        || distCategory != lastSpokenDistanceCategory;

                if (isNewObject || timeSinceLastSpeech >= SPEECH_COOLDOWN_MS) {
                    speakDetection(translatedLabel, direction, distanceText, distCategory);
                }
            }
        });
    }

    private void speakDetection(String objectName, String direction, String distanceText, DistanceEstimator.DistanceCategory distCategory) {
        String speechText = getString(R.string.detection_pattern, objectName, direction, distanceText);
        Log.d(TAG, "[TTS] Speaking: " + speechText);
        lastSpokenText = speechText;
        lastSpokenObjectName = objectName;
        lastSpokenDirection = direction;
        lastSpokenDistanceCategory = distCategory;
        lastSpeechTime = System.currentTimeMillis();
        
        ttsManager.speak(speechText, getCurrentLanguage());
    }

    private void repeatLastSpeech() {
        if (lastSpokenText != null && !lastSpokenText.isEmpty()) {
            ttsManager.speak(lastSpokenText, getCurrentLanguage());
        } else {
            ttsManager.speak(getString(R.string.no_detection_to_repeat), getCurrentLanguage());
        }
    }

    private void processFragmentVoiceCommand(String text) {
        Log.d(TAG, "[VOICE] processFragmentVoiceCommand: " + text);
        CommandParser.CommandResult result = CommandParser.parse(text);
        
        boolean navigatedOrClosed = voiceCommandDispatcher.execute(result);
        
        // Resume detector ONLY if user stays in CameraFragment
        if (!navigatedOrClosed && detectorState == DetectorState.PAUSED_BY_VOICE) {
            // Note: If intent was STOP_OBJECT_DETECTION, the dispatcher already returned true (navigates home).
            // If we are here, we are staying in CameraFragment and should resume detection.
            detectorState = DetectorState.RUNNING;
            Log.d(TAG, "[VOICE] VOICE_RESUME");
            Log.d(TAG, "[Detector] Inference resumed");
        } else if (navigatedOrClosed) {
            Log.d(TAG, "[VOICE] VOICE_PAUSE (Permanent due to navigation)");
            detectorState = DetectorState.STOPPED;
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Detector Error: " + error);
        mainHandler.post(() -> {
            if (isAdded() && !isDetached() && getView() != null) {
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "[Fragment] onResume called");
        String lang = getCurrentLanguage();
        String welcome = lang.equals("vi") ? "Nhận diện vật thể đã sẵn sàng." : "Object detection is ready.";
        ttsManager.speakNow(welcome);
        detectorState = DetectorState.RUNNING;
    }

    private String getCurrentLanguage() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "[Fragment] onPause called");
        detectorState = DetectorState.STOPPED;
        // Chặn spam TTS nếu Fragment bị ẩn đi (nhưng không nói đè nếu đang Navigate)
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "[Fragment] onStop called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "[Fragment] onDestroyView called");
        
        detectorState = DetectorState.STOPPED;
        
        if (cameraProvider != null) {
            cameraProvider.unbindAll(); // Ngắt luồng CameraX ngay lập tức
            Log.d(TAG, "[Camera] CameraX unbound");
        }
        
        if (detectorManager != null) {
            detectorManager.close();
        }
        
        if (speechRecognizerManager != null) {
            speechRecognizerManager.destroy();
        }
        
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        
        if (bitmapBuffer != null) {
            bitmapBuffer.recycle();
            bitmapBuffer = null;
        }
        
        // Ngắt các thông báo object detection còn tồn đọng trong hàng đợi TTS
        if (ttsManager != null) {
            ttsManager.clearLowPriorityQueue();
        }
    }
}
