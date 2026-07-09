package com.example.voiceassistant.ui.fragments;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.Manifest;
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
import com.example.voiceassistant.call.CallManager;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.contacts.ContactManager;
import com.example.voiceassistant.permissions.PermissionHelper;
import com.example.voiceassistant.speech.CommandParser;
import com.example.voiceassistant.speech.SpeechRecognizerManager;
import com.example.voiceassistant.tts.TTSManager;
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

        ttsManager = new TTSManager(context, new TTSManager.TTSListener() {
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
            }

            @Override
            public void onEndOfSpeech() {
                updateUI(() -> {
                    tvStatus.setText(getString(R.string.status_title));
                    stopPulseAnimation();
                });
            }

            @Override
            public void onResult(String recognizedText) {
                Log.d(TAG, "Speech Result Received: " + recognizedText);
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
                    // Nếu máy báo lỗi nhưng chúng ta đã có kết quả tạm thời khá dài (ví dụ > 5 ký tự)
                    // thì thử xử lý luôn lệnh đó thay vì báo lỗi.
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
    }

    private void setupListeners() {
        btnMicro.setOnClickListener(v -> {
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
        
        switch (result.getCommandType()) {
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
                String response = getString(R.string.error_not_understand);
                tvResponse.setText(response);
                ttsManager.speak(response);
                break;
        }
    }

    private void handleCallCommand(String contactName) {
        Log.d(TAG, "Handling CALL command for: " + contactName);
        ContactManager.ContactInfo contact = contactManager.findContactByName(contactName);
        
        if (contact != null) {
            Log.d(TAG, "Contact found: " + contact.getName() + " - " + contact.getPhoneNumber());
            String response = getString(R.string.calling_contact, contact.getName());
            tvResponse.setText(response);
            ttsManager.speak(response);
            callManager.makeCall(contact.getPhoneNumber(), contact.getName());
        } else {
            Log.w(TAG, "Contact NOT found in system: " + contactName);
            String response = getString(R.string.contact_not_found, contactName);
            tvResponse.setText(response);
            ttsManager.speak(response);
        }
    }

    private void handleTimeCommand() {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String response = getString(R.string.time_response, time);
        tvResponse.setText(response);
        ttsManager.speak(response);
    }

    private void handleBatteryCommand() {
        int batteryLevel = getBatteryLevel();
        String response = getString(R.string.battery_status, batteryLevel);
        tvResponse.setText(response);
        ttsManager.speak(response);
    }

    private int getBatteryLevel() {
        android.os.BatteryManager bm = (android.os.BatteryManager) requireContext().getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return 0;
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
        if (ttsManager != null) ttsManager.shutdown();
        handler.removeCallbacks(timeUpdater);
    }
}
