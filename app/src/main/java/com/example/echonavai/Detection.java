package com.example.echonavai;

import android.graphics.RectF;

public class Detection {
    private final String label;
    private final float confidence;
    private final RectF box;

    public Detection(String label, float confidence, RectF box) {
        this.label = label;
        this.confidence = confidence;
        this.box = box;
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    public RectF getBox() {
        return box;
    }
}