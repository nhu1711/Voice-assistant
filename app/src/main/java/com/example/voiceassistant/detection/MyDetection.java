package com.example.voiceassistant.detection;

import android.graphics.RectF;
import java.util.List;
import java.util.Collections;

public class MyDetection {
    private final RectF boundingBox;
    private final List<MyCategory> categories;

    public MyDetection(RectF boundingBox, MyCategory category) {
        this.boundingBox = boundingBox;
        this.categories = Collections.singletonList(category);
    }

    public RectF boundingBox() {
        return boundingBox;
    }

    public List<MyCategory> categories() {
        return categories;
    }
}
