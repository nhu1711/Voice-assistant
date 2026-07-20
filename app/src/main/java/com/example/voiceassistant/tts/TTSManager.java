package com.example.voiceassistant.tts;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.example.voiceassistant.constants.AppConstants;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

/**
 * Quản lý Text-to-Speech (FR-07) theo mô hình Singleton + Hàng đợi (Queue)
 */
public class TTSManager implements TextToSpeech.OnInitListener {
    
    private static final String TAG = "TTSManager";
    private static TTSManager instance;
    
    private TextToSpeech tts;
    private final Context context;
    private boolean isInitialized = false;
    private TTSListener listener;
    
    private String currentLanguage = AppConstants.DEFAULT_LANGUAGE;
    
    // Hàng đợi các câu cần đọc
    private final Queue<TTSRequest> utteranceQueue = new LinkedList<>();
    private boolean isSpeaking = false;
    private boolean isAssistantListening = false;
    
    public interface TTSListener {
        void onSpeechStarted();
        void onSpeechFinished();
        void onSpeechError(String error);
    }
    
    private static class TTSRequest {
        String text;
        String language;
        float speechRate;
        
        TTSRequest(String text, String language, float speechRate) {
            this.text = text;
            this.language = language;
            this.speechRate = speechRate;
        }
    }

    private TTSManager(Context context) {
        // Dùng applicationContext để tránh rò rỉ bộ nhớ
        this.context = context.getApplicationContext();
        this.tts = new TextToSpeech(this.context, this);
    }
    
    public static synchronized TTSManager getInstance(Context context) {
        if (instance == null) {
            instance = new TTSManager(context);
        }
        return instance;
    }
    
    public void setListener(TTSListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            setLanguage(currentLanguage);
            
            isInitialized = true;
            Log.d(TAG, "TTS initialized");
            
            // Đăng ký listener cho sự kiện
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (listener != null) listener.onSpeechStarted();
                }
                
                @Override
                public void onDone(String utteranceId) {
                    if (listener != null) listener.onSpeechFinished();
                    isSpeaking = false;
                    processNextInQueue(); // Lấy câu tiếp theo ra đọc
                }
                
                @Override
                public void onError(String utteranceId) {
                    if (listener != null) listener.onSpeechError("TTS error: " + utteranceId);
                    isSpeaking = false;
                    processNextInQueue();
                }
            });
            
            // Nếu có câu nào đang đợi trong lúc init thì xử lý luôn
            processNextInQueue();
        } else {
            Log.e(TAG, "TTS initialization failed");
        }
    }

    /**
     * Bật/Tắt chế độ Trợ lý đang nghe.
     * Nếu true, sẽ tạm dừng đọc các thông báo.
     * Nếu false, sẽ xử lý tiếp hàng đợi.
     */
    public void setAssistantListening(boolean listening) {
        this.isAssistantListening = listening;
        if (!listening) {
            processNextInQueue();
        } else {
            stop(); // Ngừng câu đang nói nếu người dùng muốn chen ngang
        }
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
        if (tts == null) return;
        
        Locale locale = language.equals(AppConstants.LANGUAGE_VIETNAMESE) 
            ? new Locale("vi", "VN") 
            : Locale.US;
        
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || 
            result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: " + language + ", using fallback");
            tts.setLanguage(Locale.US);
        }
    }
    
    /**
     * Đưa câu nói vào hàng đợi (Queue)
     */
    public synchronized void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;

        android.content.SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        float speed = prefs.getFloat(AppConstants.PREF_SPEECH_RATE, AppConstants.TTS_DEFAULT_SPEECH_RATE);
        String lang = prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
        
        utteranceQueue.add(new TTSRequest(text, lang, speed));
        processNextInQueue();
    }

    /**
     * Đọc với ngôn ngữ tự định nghĩa (Thêm vào Queue)
     */
    public synchronized void speak(String text, String language) {
        if (text == null || text.trim().isEmpty()) return;
        
        android.content.SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        float speed = prefs.getFloat(AppConstants.PREF_SPEECH_RATE, AppConstants.TTS_DEFAULT_SPEECH_RATE);
        
        utteranceQueue.add(new TTSRequest(text, language, speed));
        processNextInQueue();
    }
    
    /**
     * Đọc ngay lập tức, BỎ QUA hàng đợi (Dành riêng cho tương tác giọng nói trực tiếp)
     */
    public synchronized void speakNow(String text) {
        if (text == null || text.trim().isEmpty()) return;

        android.content.SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        float speed = prefs.getFloat(AppConstants.PREF_SPEECH_RATE, AppConstants.TTS_DEFAULT_SPEECH_RATE);
        String lang = prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
        
        // Dừng hiện tại và chèn lên đầu
        stop();
        ((LinkedList<TTSRequest>) utteranceQueue).addFirst(new TTSRequest(text, lang, speed));
        processNextInQueue();
    }

    private synchronized void processNextInQueue() {
        if (!isInitialized) return;
        if (isSpeaking) return;
        if (isAssistantListening) return; // Nếu đang chờ lệnh thoại thì không đọc thông báo
        if (utteranceQueue.isEmpty()) return;

        TTSRequest request = utteranceQueue.poll();
        if (request == null) return;

        isSpeaking = true;

        if (!currentLanguage.equals(request.language)) {
            setLanguage(request.language);
        }
        
        tts.setSpeechRate(request.speechRate);
        
        android.content.SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        float volume = prefs.getInt("volume_level", 80) / 100f;
        
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
        String utteranceId = UUID.randomUUID().toString();
        
        Log.d(TAG, "Speaking from queue: " + request.text);
        tts.speak(request.text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
    }
    
    /**
     * Dừng đọc
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
            isSpeaking = false;
        }
    }
    
    /**
     * Giải phóng tài nguyên
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        isInitialized = false;
        instance = null;
    }
    
    public boolean isReady() {
        return isInitialized;
    }
}
