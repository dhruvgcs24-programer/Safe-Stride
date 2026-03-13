package com.example.safestrider.vision;

public class TrackedObstacle {

    private final String label;
    private final String position;
    private float confidence;
    private float area;
    private int stableFrames;
    private long lastSeenTime;

    public TrackedObstacle(String label, String position, float confidence, float area,
                           int stableFrames, long lastSeenTime) {
        this.label = label;
        this.position = position;
        this.confidence = confidence;
        this.area = area;
        this.stableFrames = stableFrames;
        this.lastSeenTime = lastSeenTime;
    }

    public String getLabel() {
        return label;
    }

    public String getPosition() {
        return position;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getArea() {
        return area;
    }

    public void setArea(float area) {
        this.area = area;
    }

    public int getStableFrames() {
        return stableFrames;
    }

    public void incrementStableFrames() {
        this.stableFrames++;
    }

    public long getLastSeenTime() {
        return lastSeenTime;
    }

    public void setLastSeenTime(long lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }
}