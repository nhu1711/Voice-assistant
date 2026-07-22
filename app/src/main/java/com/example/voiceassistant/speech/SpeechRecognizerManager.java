package com.example.voiceassistant.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Quản lý Speech-to-Text (FR-01, FR-02)
 * Khởi tạo và xử lý kết quả nhận diện giọng nói
 */
public class SpeechRecognizerManager {
    
    private static final String TAG = "SpeechRecognizerManager";
    
    private SpeechRecognizer speechRecognizer;
    private final Context context;
    private final RecognitionCallback callback;
    private boolean isListening = false;
    
    public interface RecognitionCallback {
        void onReadyForSpeech();
        void onBeginningOfSpeech();
        void onEndOfSpeech();
        void onResult(String recognizedText);
        void onError(String errorMessage);
        void onPartialResult(String partialText);
    }
    
    public SpeechRecognizerManager(Context context, RecognitionCallback callback) {
        this.context = context;
        this.callback = callback;
        initSpeechRecognizer();
    }
    
    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            setListener();
        } else {
            // Force create using Google Speech Recognition service if the general one fails
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context, 
                    android.content.ComponentName.unflattenFromString("com.google.android.googlequicksearchbox/com.google.android.voicesearch.service.SpeechRecognitionService"));
                setListener();
            } catch (Exception e) {
                Log.e(TAG, "Speech recognition totally unavailable");
                if (callback != null) {
                    callback.onError(context.getString(R.string.speech_not_available));
                }
            }
        }
    }

    private void setListener() {
        if (speechRecognizer == null) return;
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                Log.d(TAG, "Ready for speech");
                if (callback != null) {
                    callback.onReadyForSpeech();
                }
            }
            
            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech");
                if (callback != null) {
                    callback.onBeginningOfSpeech();
                }
            }
            
            @Override
            public void onRmsChanged(float rmsdB) {}
            
            @Override
            public void onBufferReceived(byte[] buffer) {}
            
            @Override
            public void onEndOfSpeech() {
                if (isListening) {
                    isListening = false;
                    Log.d(TAG, "[Speech] recognition finished");
                    if (callback != null) {
                        callback.onEndOfSpeech();
                    }
                }
            }
            
            @Override
            public void onError(int error) {
                String errorMessage = getErrorMessage(error);
                Log.e(TAG, "Speech recognition error: " + errorMessage);
                if (isListening) {
                    isListening = false;
                    Log.d(TAG, "[Speech] recognition finished (error)");
                    if (callback != null) {
                        callback.onEndOfSpeech();
                    }
                }
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
            
            @Override
            public void onResults(Bundle results) {
                if (isListening) {
                    isListening = false;
                    Log.d(TAG, "[Speech] recognition finished (results)");
                    if (callback != null) {
                        callback.onEndOfSpeech();
                    }
                }
                
                ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                );
                
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d(TAG, "Recognized: " + recognizedText);
                    if (callback != null) {
                        callback.onResult(recognizedText);
                    }
                } else {
                    if (callback != null) {
                        callback.onError(context.getString(R.string.speech_no_match));
                    }
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                );
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onPartialResult(matches.get(0));
                }
            }
            
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }
    
    /**
     * Bắt đầu lắng nghe (FR-01)
     */
    public void startListening() {
        if (speechRecognizer == null) {
            initSpeechRecognizer();
        }
        
        // Luôn cancel và stop các phiên cũ để tránh lỗi "Hệ thống bận"
        speechRecognizer.cancel();
        isListening = false;
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        
        // Lấy ngôn ngữ hiện tại của hệ thống để nhận diện chính xác
        Locale currentLocale = context.getResources().getConfiguration().getLocales().get(0);
        String languageTag = currentLocale.toLanguageTag(); // Ví dụ: "vi-VN" hoặc "en-US"
        
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageTag);
        
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt));
        intent.putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
            AppConstants.SPEECH_TIMEOUT
        );
        
        speechRecognizer.startListening(intent);
        Log.d(TAG, "Started listening");
    }
    
    /**
     * Dừng lắng nghe
     */
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "Stopped listening");
            if (callback != null) {
                callback.onEndOfSpeech();
            }
        }
    }
    
    /**
     * Hủy nhận diện
     */
    public void cancel() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            if (isListening) {
                isListening = false;
                Log.d(TAG, "Cancelled recognition");
                if (callback != null) {
                    callback.onEndOfSpeech();
                }
            }
        }
    }
    
    /**
     * Giải phóng tài nguyên
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
            Log.d(TAG, "SpeechRecognizer destroyed");
        }
    }
    
    /**
     * Lấy thông báo lỗi
     */
    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return context.getString(R.string.speech_error_audio);
            case SpeechRecognizer.ERROR_CLIENT:
                return context.getString(R.string.speech_error_client);
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return context.getString(R.string.speech_error_insufficient_permissions);
            case SpeechRecognizer.ERROR_NETWORK:
                return context.getString(R.string.speech_error_network);
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return context.getString(R.string.speech_error_network_timeout);
            case SpeechRecognizer.ERROR_NO_MATCH:
                return context.getString(R.string.speech_error_no_match);
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return context.getString(R.string.speech_error_recognizer_busy);
            case SpeechRecognizer.ERROR_SERVER:
                return context.getString(R.string.speech_error_server);
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return context.getString(R.string.speech_error_speech_timeout);
            default:
                return context.getString(R.string.speech_error_unknown, error);
        }
    }
    
    public boolean isListening() {
        return isListening;
    }
}
