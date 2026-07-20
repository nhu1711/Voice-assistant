package com.example.voiceassistant.ui.fragments;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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
public class CameraFragment extends Fragment implements ObjectDetectorManager.DetectorListener {

    private static final String TAG = "CameraFragment";
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
    private boolean isDetectionActive = true;

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
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
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

        initSpeechRecognizer();
        
        contactManager = new ContactManager(context);
        callManager = new CallManager(context, ttsManager);
        batteryManagerHelper = new BatteryManagerHelper(context);
    }

    private void initSpeechRecognizer() {
        speechRecognizerManager = new SpeechRecognizerManager(requireContext(), new SpeechRecognizerManager.RecognitionCallback() {
            @Override
            public void onReadyForSpeech() {
                mainHandler.post(() -> {
                    tvDetectionResult.setText("Đang lắng nghe câu lệnh...");
                });
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onResult(String recognizedText) {
                mainHandler.post(() -> {
                    processFragmentVoiceCommand(recognizedText);
                });
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "Lỗi giọng nói: " + errorMessage, Toast.LENGTH_SHORT).show();
                    if (isDetectionActive) {
                        tvDetectionResult.setText("Đang nhận diện...");
                    } else {
                        tvDetectionResult.setText("Đã dừng nhận diện");
                    }
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                mainHandler.post(() -> {
                    tvDetectionResult.setText(partialText);
                });
            }
        });
    }

    private void setupListeners() {
        btnMicro.setOnClickListener(v -> {
            if (!PermissionHelper.hasRecordAudioPermission(requireContext())) {
                PermissionHelper.requestRecordAudioPermission(requireActivity());
                return;
            }
            
            if (speechRecognizerManager.isListening()) {
                speechRecognizerManager.stopListening();
                if (isDetectionActive) {
                    tvDetectionResult.setText("Đang nhận diện...");
                } else {
                    tvDetectionResult.setText("Đã dừng nhận diện");
                }
            } else {
                speechRecognizerManager.startListening();
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
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

            if (!isDetectionActive) {
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
            if (!isAdded()) return;

            // Truyền kết quả hiển thị cho overlay vẽ khung viền
            detectionOverlay.setResults(detections, imageHeight, imageWidth);
            Log.d(TAG, "Overlay updated with " + objectsCount + " bounding boxes.");

            if (detections == null || detections.isEmpty()) {
                tvDetectionResult.setText("Không phát hiện vật thể");
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
                String viLabel = LabelTranslator.translate(enLabel);
                float score = category.score();

                // Tính toán hướng (trái, phải, phía trước) dựa trên tâm X của vật thể
                RectF box = prominentDetection.boundingBox();
                float centerXNormalized = (box.left + box.right) / 2.0f / imageWidth;

                String direction;
                if (centerXNormalized < 0.35f) {
                    direction = "bên trái";
                } else if (centerXNormalized > 0.65f) {
                    direction = "bên phải";
                } else {
                    direction = "phía trước";
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

                String distanceText = DistanceEstimator.getDistanceSpeech(distCategory);
                
                // Hiển thị kết quả văn bản
                String displayResult = String.format("Phát hiện %s %s, %s (%.0f%%)", viLabel, direction, distanceText, score * 100);
                tvDetectionResult.setText(displayResult);

                // Kiểm soát cooldown chống spam phát thanh
                long currentTime = System.currentTimeMillis();
                long timeSinceLastSpeech = currentTime - lastSpeechTime;

                boolean isNewObject = !viLabel.equals(lastSpokenObjectName) 
                        || !direction.equals(lastSpokenDirection) 
                        || distCategory != lastSpokenDistanceCategory;

                if (isNewObject || timeSinceLastSpeech >= SPEECH_COOLDOWN_MS) {
                    speakDetection(viLabel, direction, distanceText, distCategory);
                }
            }
        });
    }

    private void speakDetection(String objectName, String direction, String distanceText, DistanceEstimator.DistanceCategory distCategory) {
        String speechText = String.format("Có %s %s, %s", objectName, direction, distanceText);
        Log.d(TAG, "[TTS] Speaking: " + speechText);
        lastSpokenText = speechText;
        lastSpokenObjectName = objectName;
        lastSpokenDirection = direction;
        lastSpokenDistanceCategory = distCategory;
        lastSpeechTime = System.currentTimeMillis();
        
        ttsManager.speak(speechText, "vi");
    }

    private void repeatLastSpeech() {
        if (lastSpokenText != null && !lastSpokenText.isEmpty()) {
            ttsManager.speak(lastSpokenText, "vi");
        } else {
            ttsManager.speak("Chưa phát hiện vật thể nào để đọc lại", "vi");
        }
    }

    private void processFragmentVoiceCommand(String text) {
        Log.d(TAG, "Speech command received: " + text);
        CommandParser.CommandResult result = CommandParser.parse(text);
        Log.d(TAG, "Command parsed: " + result.getCommandType() + ", param: " + result.getCommandText());
        
        switch (result.getCommandType()) {
            case AppConstants.COMMAND_START_DETECT:
                isDetectionActive = true;
                ttsManager.speak("Bắt đầu nhận diện vật thể", "vi");
                tvDetectionResult.setText("Đang nhận diện...");
                break;
                
            case AppConstants.COMMAND_STOP_DETECT:
                isDetectionActive = false;
                detectionOverlay.clear();
                ttsManager.speak("Đã dừng nhận diện vật thể", "vi");
                tvDetectionResult.setText("Đã dừng nhận diện");
                break;
                
            case AppConstants.COMMAND_CLOSE_CAMERA:
                ttsManager.speak("Đóng camera", "vi");
                // Quay lại màn hình Home
                if (getActivity() instanceof MainActivity) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    }
                }
                break;
                
            case AppConstants.COMMAND_REPEAT:
                repeatLastSpeech();
                break;

            case AppConstants.COMMAND_DETECT:
                ttsManager.speak("Chế độ nhận diện vật thể đang mở", "vi");
                break;
                
            case AppConstants.COMMAND_CALL:
                handleCallCommand(result.getCommandText());
                break;
                
            case AppConstants.COMMAND_TIME:
                handleTimeCommand();
                break;
                
            case AppConstants.COMMAND_BATTERY:
                handleBatteryCommand();
                break;
                
            default:
                String response = "Tôi không hiểu lệnh này";
                ttsManager.speak(response, "vi");
                tvDetectionResult.setText(response);
                break;
        }
    }

    private void handleCallCommand(String contactName) {
        Log.d(TAG, "Handling CALL command for: " + contactName);
        ContactManager.ContactInfo contact = contactManager.findContactByName(contactName);
        
        if (contact != null) {
            String response = getString(R.string.calling_contact, contact.getName());
            tvDetectionResult.setText(response);
            ttsManager.speak(response, "vi");
            callManager.makeCall(contact.getPhoneNumber(), contact.getName());
        } else {
            String response = getString(R.string.contact_not_found, contactName);
            tvDetectionResult.setText(response);
            ttsManager.speak(response, "vi");
        }
    }

    private void handleTimeCommand() {
        try {
            Context context = requireContext();
            String displayText = TimeFormatter.getDisplayTime(context);
            String speechText = TimeFormatter.getTimeSpeech(context, AppConstants.LANGUAGE_VIETNAMESE);
            
            tvDetectionResult.setText(displayText);
            ttsManager.speak(speechText, "vi");
        } catch (Exception e) {
            Log.e(TAG, "Error handling time command", e);
            String errorMsg = getString(R.string.error_time);
            tvDetectionResult.setText(errorMsg);
            ttsManager.speak(errorMsg, "vi");
        }
    }

    private void handleBatteryCommand() {
        String response = batteryManagerHelper.getBatterySpeechResponse();
        tvDetectionResult.setText(getString(R.string.battery_status, batteryManagerHelper.getBatteryLevel()));
        ttsManager.speak(response, AppConstants.LANGUAGE_VIETNAMESE);
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Detector Error: " + error);
        mainHandler.post(() -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isDetectionActive = false;
        if (detectorManager != null) {
            detectorManager.close();
        }
        if (speechRecognizerManager != null) {
            speechRecognizerManager.destroy();
        }
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (bitmapBuffer != null) {
            bitmapBuffer.recycle();
            bitmapBuffer = null;
        }
    }
}
