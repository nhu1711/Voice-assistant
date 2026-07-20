package com.example.voiceassistant.ui.fragments;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voiceassistant.R;
import com.example.voiceassistant.battery.BatteryManagerHelper;
import com.example.voiceassistant.call.CallManager;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.contacts.ContactManager;
import com.example.voiceassistant.permissions.PermissionHelper;
import com.example.voiceassistant.speech.CommandParser;
import com.example.voiceassistant.speech.SpeechRecognizerManager;
import com.example.voiceassistant.speech.VoiceCommandDispatcher;
import com.example.voiceassistant.speech.VoiceIntent;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.utils.TimeFormatter;
import com.example.voiceassistant.ui.activities.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private MaterialButton btnMicro;
    private MaterialButton btnSOS;
    private TextView tvStatus;
    private TextView tvCommand;
    private TextView tvResponse;
    private TextView tvTime;
    private TextView tvBattery;

    private TTSManager ttsManager;
    private SpeechRecognizerManager speechRecognizerManager;
    private ContactManager contactManager;
    private CallManager callManager;
    private BatteryManagerHelper batteryManagerHelper;

    private ObjectAnimator pulseAnimator;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            updateSystemInfo();
            handler.postDelayed(this, 60000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(view);
        initManagers();
        setupListeners();
        updateSystemInfo();
        handler.post(timeUpdater);
        return view;
    }

    private void initViews(View view) {
        btnMicro = view.findViewById(R.id.btn_micro);
        btnSOS = view.findViewById(R.id.btn_sos);
        tvStatus = view.findViewById(R.id.tv_status);
        tvCommand = view.findViewById(R.id.tv_command);
        tvResponse = view.findViewById(R.id.tv_response);
        tvTime = view.findViewById(R.id.tv_time);
        tvBattery = view.findViewById(R.id.tv_battery);
    }

    private VoiceCommandDispatcher voiceCommandDispatcher;
    private void initManagers() {
        Context context = requireContext();
        
        // Setup Pulse Animation
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                btnMicro,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f)
        );
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

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

        speechRecognizerManager = new SpeechRecognizerManager(context, new SpeechRecognizerManager.RecognitionCallback() {
            @Override
            public void onReadyForSpeech() {
                updateUI(() -> {
                    tvStatus.setText(R.string.response_hint);
                    startPulseAnimation();
                });
            }

            @Override
            public void onBeginningOfSpeech() {
                updateUI(() -> tvStatus.setText(getString(R.string.analyzing)));
                ttsManager.setAssistantListening(true);
            }

            @Override
            public void onEndOfSpeech() {
                updateUI(() -> {
                    tvStatus.setText(getString(R.string.status_title));
                    stopPulseAnimation();
                });
                ttsManager.setAssistantListening(false);
            }

            @Override
            public void onResult(String recognizedText) {
                Log.d(TAG, "[VOICE] VOICE_RESULT: " + recognizedText);
                updateUI(() -> {
                    stopPulseAnimation();
                    tvCommand.setText(recognizedText);
                    tvStatus.setText(getString(R.string.status_title));
                    processVoiceCommand(recognizedText);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Speech Error: " + errorMessage);
                updateUI(() -> {
                    stopPulseAnimation();
                    String currentText = tvCommand.getText().toString();
                    if (currentText.length() > 5) {
                        tvStatus.setText(getString(R.string.status_title));
                        processVoiceCommand(currentText);
                    } else if (currentText.isEmpty()) {
                        tvStatus.setText(errorMessage);
                    }
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                updateUI(() -> tvCommand.setText(partialText));
            }
        });

        contactManager = new ContactManager(context);
        callManager = new CallManager(context, ttsManager);
        batteryManagerHelper = new BatteryManagerHelper(context);

        voiceCommandDispatcher = new VoiceCommandDispatcher(requireActivity(), ttsManager, contactManager, callManager, batteryManagerHelper, new VoiceCommandDispatcher.CommandCallback() {
            @Override
            public void onResponse(String text) {
                updateUI(() -> tvResponse.setText(text));
            }
        });
    }

    private void setupListeners() {
        btnMicro.setOnClickListener(v -> {
            Log.d(TAG, "[VOICE] VOICE_START");
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

        btnSOS.setOnClickListener(v -> {
            ttsManager.speak(getString(R.string.sos_button));
            // SOS implementation will come in next phase
        });
    }

    private void startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(getContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show();
            return;
        }
        // Làm sạch ô văn bản trước khi nghe mới
        tvCommand.setText("");
        tvResponse.setText("");
        
        ttsManager.stop();
        speechRecognizerManager.startListening();
    }

    private void stopVoiceRecognition() {
        speechRecognizerManager.stopListening();
        stopPulseAnimation();
        tvStatus.setText(getString(R.string.status_title));
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

    private void processVoiceCommand(String text) {
        CommandParser.CommandResult result = CommandParser.parse(text);
        voiceCommandDispatcher.execute(result);
    }

    private void handleDetectCommand() {
        Log.d(TAG, "Handling DETECT command");
        String response = "Đang mở chế độ nhận diện vật thể.";
        tvResponse.setText(response);
        ttsManager.speak(response);
        
        handler.postDelayed(() -> {
            if (isAdded() && getActivity() instanceof MainActivity) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_object_detection);
                }
            }
        }, 1500);
    }

    private int getBatteryLevel() {
        return batteryManagerHelper.getBatteryLevel();
    }

    private void updateSystemInfo() {
        if (isAdded()) {
            tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            tvBattery.setText(getString(R.string.battery_status, getBatteryLevel()));
        }
    }

    private void updateUI(Runnable runnable) {
        if (isAdded()) {
            handler.post(runnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizerManager != null) speechRecognizerManager.destroy();
        handler.removeCallbacks(timeUpdater);
    }
}
