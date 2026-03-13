package com.example.safestrider.vision;

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

    public float getCenterX() {
        return box.centerX();
    }

    public float getCenterY() {
        return box.centerY();
    }

    public float getArea() {
        return box.width() * box.height();
    }
}