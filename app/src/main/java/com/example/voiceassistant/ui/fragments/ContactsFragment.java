package com.example.voiceassistant.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voiceassistant.R;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import com.google.android.material.button.MaterialButton;
import com.example.voiceassistant.permissions.PermissionHelper;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

public class ContactsFragment extends Fragment {

    private com.example.voiceassistant.speech.SpeechRecognizerManager speechRecognizerManager;
    private com.example.voiceassistant.speech.VoiceCommandDispatcher voiceCommandDispatcher;
    private com.example.voiceassistant.tts.TTSManager ttsManager;
    private android.widget.TextView tvCommand;
    private MaterialButton btnMicro;
    private android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private ObjectAnimator pulseAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        tvCommand = view.findViewById(R.id.tv_command);
        btnMicro = view.findViewById(R.id.btn_micro);
        
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                btnMicro,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f)
        );
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        ttsManager = com.example.voiceassistant.tts.TTSManager.getInstance(requireContext());
        voiceCommandDispatcher = new com.example.voiceassistant.speech.VoiceCommandDispatcher(
                requireActivity(), ttsManager,
                new com.example.voiceassistant.contacts.ContactManager(requireContext()),
                new com.example.voiceassistant.call.CallManager(requireContext(), ttsManager),
                new com.example.voiceassistant.battery.BatteryManagerHelper(requireContext()),
                text -> mainHandler.post(() -> tvCommand.setText(text))
        );
        
        speechRecognizerManager = new com.example.voiceassistant.speech.SpeechRecognizerManager(requireContext(), new com.example.voiceassistant.speech.SpeechRecognizerManager.RecognitionCallback() {
            @Override public void onReadyForSpeech() { 
                mainHandler.post(() -> {
                    tvCommand.setText(R.string.status_listening);
                    pulseAnimator.start();
                }); 
            }
            @Override public void onBeginningOfSpeech() { 
                mainHandler.post(() -> ttsManager.setAssistantListening(true)); 
            }
            @Override public void onEndOfSpeech() { 
                mainHandler.post(() -> {
                    ttsManager.setAssistantListening(false);
                    pulseAnimator.cancel();
                    btnMicro.setScaleX(1f);
                    btnMicro.setScaleY(1f);
                }); 
            }
            @Override public void onResult(String text) {
                mainHandler.post(() -> {
                    pulseAnimator.cancel();
                    btnMicro.setScaleX(1f);
                    btnMicro.setScaleY(1f);
                    tvCommand.setText(text);
                    voiceCommandDispatcher.execute(com.example.voiceassistant.speech.CommandParser.parse(text));
                });
            }
            @Override public void onError(String error) { 
                mainHandler.post(() -> {
                    pulseAnimator.cancel();
                    btnMicro.setScaleX(1f);
                    btnMicro.setScaleY(1f);
                    
                    String currentText = tvCommand.getText().toString();
                    if (currentText.length() > 5) {
                        tvCommand.setText(currentText);
                        voiceCommandDispatcher.execute(com.example.voiceassistant.speech.CommandParser.parse(currentText));
                    } else if (currentText.isEmpty()) {
                        tvCommand.setText(error); 
                    }
                });
            }
            @Override public void onPartialResult(String partialText) { mainHandler.post(() -> tvCommand.setText(partialText)); }
        });
        
        btnMicro.setOnClickListener(v -> {
            if (!PermissionHelper.hasRecordAudioPermission(requireContext())) {
                PermissionHelper.requestRecordAudioPermission(requireActivity());
                return;
            }
            if (speechRecognizerManager.isListening()) {
                speechRecognizerManager.stopListening();
                pulseAnimator.cancel();
                btnMicro.setScaleX(1f);
                btnMicro.setScaleY(1f);
            } else {
                if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                    Toast.makeText(getContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                tvCommand.setText("");
                ttsManager.stop();
                speechRecognizerManager.startListening();
            }
        });
        
        return view;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechRecognizerManager != null) {
            speechRecognizerManager.destroy();
        }
    }
}
