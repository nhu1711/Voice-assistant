package com.example.voiceassistant.tts;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.example.voiceassistant.constants.AppConstants;

import java.util.Locale;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Quản lý Text-to-Speech (FR-07) theo mô hình Singleton + Hàng đợi Ưu tiên (PriorityQueue)
 */
public class TTSManager implements TextToSpeech.OnInitListener {
    
    private static final String TAG = "TTSManager";
    private static TTSManager instance;
    
    private TextToSpeech tts;
    private final Context context;
    private boolean isInitialized = false;
    private TTSListener listener;
    
    private String currentLanguage = AppConstants.DEFAULT_LANGUAGE;
    
    // Hàng đợi ưu tiên
    private final PriorityQueue<TTSRequest> utteranceQueue = new PriorityQueue<>((r1, r2) -> {
        // Mức độ ưu tiên cao hơn (giá trị lớn hơn) sẽ đứng trước
        int priorityCompare = Integer.compare(r2.priority.getValue(), r1.priority.getValue());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // Nếu cùng độ ưu tiên, cái nào vào trước (timestamp nhỏ hơn) đứng trước
        return Long.compare(r1.timestamp, r2.timestamp);
    });
    
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
        TTSPriority priority;
        long timestamp;
        
        TTSRequest(String text, String language, float speechRate, TTSPriority priority) {
            this.text = text;
            this.language = language;
            this.speechRate = speechRate;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
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
            Log.d(TAG, "[TTS] initialized");
            
            // Đăng ký listener cho sự kiện
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (listener != null) listener.onSpeechStarted();
                }
                
                @Override
                public void onDone(String utteranceId) {
                    Log.d(TAG, "[TTS] finished: " + utteranceId);
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
            stop(); // Ngừng câu đang nói nếu người dùng muốn nghe
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
    
    private String lastSpokenText = "";
    
    /**
     * Đưa câu nói vào hàng đợi (Queue) mặc định là mức LOW (Dành cho Object Detection)
     */
    public synchronized void speak(String text) {
        speakWithPriority(text, currentLanguage, TTSPriority.LOW);
    }

    /**
     * Đọc với ngôn ngữ tự định nghĩa mức LOW
     */
    public synchronized void speak(String text, String language) {
        speakWithPriority(text, language, TTSPriority.LOW);
    }
    
    /**
     * Lệnh đọc tương tác trực tiếp (Dành cho phản hồi sau khi nghe) - Mức NORMAL
     */
    public synchronized void speakNow(String text) {
        speakWithPriority(text, currentLanguage, TTSPriority.NORMAL);
    }
    
    /**
     * Thông báo khẩn cấp - Mức HIGH
     */
    public synchronized void speakEmergency(String text) {
        speakWithPriority(text, currentLanguage, TTSPriority.HIGH);
    }
    
    private synchronized void speakWithPriority(String text, String language, TTSPriority priority) {
        if (text == null || text.trim().isEmpty()) return;
        if (priority != TTSPriority.LOW) {
            this.lastSpokenText = text;
        }

        android.content.SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        float speed = prefs.getFloat(AppConstants.PREF_SPEECH_RATE, AppConstants.TTS_DEFAULT_SPEECH_RATE);
        
        Log.d(TAG, "[TTS Queue] add: " + text + " (Priority: " + priority + ")");
        utteranceQueue.add(new TTSRequest(text, language, speed, priority));
        
        // Nếu có ưu tiên cao hơn đang vào, ta có thể ngắt cái đang nói nếu nó ở ưu tiên thấp hơn
        if (isSpeaking && currentPriority != null && priority.getValue() > currentPriority.getValue()) {
            stop();
        }
        
        processNextInQueue();
    }

    private TTSPriority currentPriority = null;

    private synchronized void processNextInQueue() {
        if (!isInitialized) return;
        if (isSpeaking) return;
        if (isAssistantListening) return; // Nếu đang chờ lệnh thoại thì không đọc thông báo
        if (utteranceQueue.isEmpty()) {
            currentPriority = null;
            return;
        }

        TTSRequest request = utteranceQueue.poll();
        if (request == null) return;

        isSpeaking = true;
        currentPriority = request.priority;

        if (!currentLanguage.equals(request.language)) {
            setLanguage(request.language);
        }
        
        tts.setSpeechRate(request.speechRate);
        
        android.content.SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        float volume = prefs.getInt("volume_level", 80) / 100f;
        
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
        String utteranceId = UUID.randomUUID().toString();
        
        Log.d(TAG, "[TTS] speaking: " + request.text);
        Log.d(TAG, "[TTS Queue] remove (size: " + utteranceQueue.size() + ")");
        tts.speak(request.text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
    }
    
    public synchronized void repeatLastSpeech() {
        if (lastSpokenText != null && !lastSpokenText.isEmpty()) {
            speakNow(lastSpokenText);
        }
    }
    
    /**
     * Dừng đọc
     */
    public synchronized void stop() {
        if (tts != null) {
            tts.stop();
            isSpeaking = false;
            currentPriority = null;
        }
    }
    
    /**
     * Xóa toàn bộ các thông báo ưu tiên thấp (như Object Detection) khỏi hàng đợi.
     */
    public synchronized void clearLowPriorityQueue() {
        utteranceQueue.removeIf(request -> request.priority == TTSPriority.LOW);
        Log.d(TAG, "[TTS Queue] Low priority queue cleared. Size now: " + utteranceQueue.size());
        
        // Nếu hiện tại đang nói một câu LOW priority, ta cũng ngắt luôn
        if (isSpeaking && currentPriority == TTSPriority.LOW) {
            stop();
            processNextInQueue();
        }
    }
    
    /**
     * Dừng và xóa toàn bộ hàng đợi
     */
    public synchronized void clearAndStop() {
        if (tts != null) {
            tts.stop();
            isSpeaking = false;
            currentPriority = null;
            utteranceQueue.clear();
        }
    }
    
    /**
     * Giải phóng tài nguyên (CHỈ GỌI KHI ỨNG DỤNG TẮT)
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
