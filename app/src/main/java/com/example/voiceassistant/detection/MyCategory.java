package com.example.voiceassistant.detection;

public class MyCategory {
    private final String categoryName;
    private final float score;

    public MyCategory(String categoryName, float score) {
        this.categoryName = categoryName;
        this.score = score;
    }

    public String categoryName() {
        return categoryName;
    }

    public float score() {
        return score;
    }
}
