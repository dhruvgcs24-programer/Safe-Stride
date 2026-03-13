package com.example.safestrider.vision;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetectionTracker {

    private final Map<String, TrackedObstacle> trackedMap = new HashMap<>();

    private static final int MIN_STABLE_FRAMES = 3;
    private static final long STALE_TIMEOUT_MS = 2500;

    public TrackedObstacle updateAndGetBestStableObstacle(List<Detection> detections, float screenWidth) {
        long now = System.currentTimeMillis();

        removeStale(now);

        if (detections == null || detections.isEmpty()) {
            return getBestStableTrackedObstacle();
        }

        for (Detection detection : detections) {
            String label = IndoorObjectHelper.userFriendlyLabel(detection.getLabel());
            String position = SafetyEvaluator.getPosition(detection.getBox(), screenWidth);
            float confidence = detection.getConfidence();
            float area = detection.getArea();

            String key = (label + "_" + position).toLowerCase(Locale.US);

            if (trackedMap.containsKey(key)) {
                TrackedObstacle tracked = trackedMap.get(key);
                tracked.incrementStableFrames();
                tracked.setConfidence(confidence);
                tracked.setArea(area);
                tracked.setLastSeenTime(now);
            } else {
                trackedMap.put(key, new TrackedObstacle(
                        label,
                        position,
                        confidence,
                        area,
                        1,
                        now
                ));
            }
        }

        return getBestStableTrackedObstacle();
    }

    private void removeStale(long now) {
        trackedMap.entrySet().removeIf(entry ->
                now - entry.getValue().getLastSeenTime() > STALE_TIMEOUT_MS
        );
    }

    private TrackedObstacle getBestStableTrackedObstacle() {
        TrackedObstacle best = null;
        float bestScore = -1f;

        for (TrackedObstacle obstacle : trackedMap.values()) {
            if (obstacle.getStableFrames() < MIN_STABLE_FRAMES) continue;

            float score = obstacle.getConfidence();

            if ("ahead".equalsIgnoreCase(obstacle.getPosition())) {
                score += 0.4f;
            }

            if (IndoorObjectHelper.isHighPriorityLabel(obstacle.getLabel())) {
                score += 0.4f;
            }

            score += Math.min(obstacle.getArea() / 100000f, 0.5f);

            if (score > bestScore) {
                bestScore = score;
                best = obstacle;
            }
        }

        return best;
    }

    public void clear() {
        trackedMap.clear();
    }
}