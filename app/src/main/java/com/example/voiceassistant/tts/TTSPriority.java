package com.example.voiceassistant.tts;

public enum TTSPriority {
    HIGH(3),    // Emergency alerts
    NORMAL(2),  // Voice command responses
    LOW(1);     // Object detection announcements

    private final int value;

    TTSPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
