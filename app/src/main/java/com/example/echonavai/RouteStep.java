package com.example.echonavai;

public class RouteStep {
    private String instruction;
    private String landmark;
    private long timestamp;

    public RouteStep(String instruction, String landmark, long timestamp) {
        this.instruction = instruction;
        this.landmark = landmark;
        this.timestamp = timestamp;
    }

    public String getInstruction() {
        return instruction;
    }

    public String getLandmark() {
        return landmark;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}