package com.example.voiceassistant.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.example.voiceassistant.constants.AppConstants;

import java.util.Locale;

/**
 * Quản lý Text-to-Speech (FR-07)
 * Mọi phản hồi đều phải qua class này
 */
public class TTSManager implements TextToSpeech.OnInitListener {
    
    private static final String TAG = "TTSManager";
    private static final String UTTERANCE_ID = "tts_utterance";
    
    private TextToSpeech tts;
    private final Context context;
    private boolean isInitialized = false;
    private TTSListener listener;
    
    private String currentLanguage = AppConstants.DEFAULT_LANGUAGE;
    
    public interface TTSListener {
        void onSpeechStarted();
        void onSpeechFinished();
        void onSpeechError(String error);
    }
    
    public TTSManager(Context context) {
        this.context = context;
        this.tts = new TextToSpeech(context, this);
    }
    
    public TTSManager(Context context, TTSListener listener) {
        this(context);
        this.listener = listener;
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            setLanguage(currentLanguage);
            
            // Cài đặt tốc độ và pitch mặc định
            tts.setSpeechRate(AppConstants.TTS_DEFAULT_SPEECH_RATE);
            tts.setPitch(AppConstants.TTS_DEFAULT_PITCH);
            
            isInitialized = true;
            Log.d(TAG, "TTS initialized successfully");
            
            // Đăng ký listener cho sự kiện
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (listener != null) {
                        listener.onSpeechStarted();
                    }
                }
                
                @Override
                public void onDone(String utteranceId) {
                    if (listener != null) {
                        listener.onSpeechFinished();
                    }
                }
                
                @Override
                public void onError(String utteranceId) {
                    if (listener != null) {
                        listener.onSpeechError("TTS error: " + utteranceId);
                    }
                }
            });
        } else {
            Log.e(TAG, "TTS initialization failed");
        }
    }

    /**
     * Đặt ngôn ngữ mặc định cho TTS
     */
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
     * Đọc văn bản bằng TTS (BR-04)
     * @param text Văn bản cần đọc
     */
    public void speak(String text) {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized, waiting...");
            // Re-init if needed? or just wait. Plan says re-init.
            tts = new TextToSpeech(context, this);
            return;
        }
        
        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Empty text to speak");
            return;
        }
        
        Log.d(TAG, "Speaking: " + text);
        
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    /**
     * Đọc văn bản với ngôn ngữ cụ thể
     */
    public void speak(String text, String language) {
        if (!isInitialized) {
            speak(text);
            return;
        }
        
        String previousLanguage = currentLanguage;
        setLanguage(language);
        speak(text);
        // Lưu ý: Việc reset ngay lập tức có thể gây lỗi nếu engine chưa kịp đọc. 
        // Trong dự án đơn giản này, ta chấp nhận reset sau khi gọi.
        setLanguage(previousLanguage);
    }
    
    /**
     * Đọc văn bản với tốc độ tùy chỉnh
     */
    public void speak(String text, float speechRate) {
        if (!isInitialized) {
            speak(text);
            return;
        }
        tts.setSpeechRate(speechRate);
        speak(text);
        tts.setSpeechRate(AppConstants.TTS_DEFAULT_SPEECH_RATE); // Reset
    }
    
    /**
     * Dừng đọc
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
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
    }
    
    /**
     * Kiểm tra TTS đã sẵn sàng chưa
     */
    public boolean isReady() {
        return isInitialized;
    }
}
