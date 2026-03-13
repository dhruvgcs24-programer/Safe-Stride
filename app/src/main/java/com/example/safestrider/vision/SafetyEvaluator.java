package com.example.safestrider.vision;

import android.graphics.RectF;

import java.util.List;
import java.util.Locale;

public class SafetyEvaluator {

    public static String evaluatePath(List<Detection> detections, float screenWidth, float screenHeight) {

        if (detections == null || detections.isEmpty()) {
            return "Path ahead looks clear.";
        }

        Detection leftNearest = null;
        Detection centerNearest = null;
        Detection rightNearest = null;

        for (Detection detection : detections) {
            String position = getPosition(detection.getBox(), screenWidth);

            if ("left".equals(position)) {
                leftNearest = pickNearest(leftNearest, detection);
            } else if ("right".equals(position)) {
                rightNearest = pickNearest(rightNearest, detection);
            } else {
                centerNearest = pickNearest(centerNearest, detection);
            }
        }

        if (centerNearest != null) {
            String label = IndoorObjectHelper.userFriendlyLabel(centerNearest.getLabel());
            float distance = estimateDistanceMeters(centerNearest, screenWidth, screenHeight);
            String distanceText = formatDistance(distance);

            boolean leftClear = leftNearest == null;
            boolean rightClear = rightNearest == null;

            if (leftClear && !rightClear) {
                return label + " ahead about " + distanceText + ". Move left.";
            }

            if (rightClear && !leftClear) {
                return label + " ahead about " + distanceText + ". Move right.";
            }

            if (leftClear && rightClear) {
                if (centerNearest.getCenterX() < screenWidth / 2f) {
                    return label + " ahead about " + distanceText + ". Move right.";
                } else {
                    return label + " ahead about " + distanceText + ". Move left.";
                }
            }

            return label + " ahead about " + distanceText + ". Stop or move carefully.";
        }

        if (leftNearest != null && rightNearest == null) {
            String label = IndoorObjectHelper.userFriendlyLabel(leftNearest.getLabel());
            float distance = estimateDistanceMeters(leftNearest, screenWidth, screenHeight);
            return label + " on the left about " + formatDistance(distance) + ". Move slightly right.";
        }

        if (rightNearest != null && leftNearest == null) {
            String label = IndoorObjectHelper.userFriendlyLabel(rightNearest.getLabel());
            float distance = estimateDistanceMeters(rightNearest, screenWidth, screenHeight);
            return label + " on the right about " + formatDistance(distance) + ". Move slightly left.";
        }

        if (leftNearest != null && rightNearest != null) {
            String leftLabel = IndoorObjectHelper.userFriendlyLabel(leftNearest.getLabel());
            String rightLabel = IndoorObjectHelper.userFriendlyLabel(rightNearest.getLabel());

            float leftDistance = estimateDistanceMeters(leftNearest, screenWidth, screenHeight);
            float rightDistance = estimateDistanceMeters(rightNearest, screenWidth, screenHeight);

            if (Math.abs(leftDistance - rightDistance) < 0.5f) {
                return leftLabel + " on the left and " + rightLabel + " on the right. Path ahead is clear. Move forward carefully.";
            }

            if (leftDistance > rightDistance) {
                return rightLabel + " on the right about " + formatDistance(rightDistance) + ". Keep slightly left and move forward.";
            } else {
                return leftLabel + " on the left about " + formatDistance(leftDistance) + ". Keep slightly right and move forward.";
            }
        }

        return "Path ahead looks clear.";
    }

    private static Detection pickNearest(Detection current, Detection candidate) {
        if (current == null) return candidate;
        return candidate.getArea() > current.getArea() ? candidate : current;
    }

    public static String getPosition(RectF box, float screenWidth) {
        float leftBoundary = screenWidth * 0.30f;
        float rightBoundary = screenWidth * 0.70f;

        if (box.left < rightBoundary && box.right > leftBoundary) {
            return "ahead";
        }

        float centerX = box.centerX();

        if (centerX < leftBoundary) {
            return "left";
        } else if (centerX > rightBoundary) {
            return "right";
        } else {
            return "ahead";
        }
    }

    public static float estimateDistanceMeters(Detection detection, float screenWidth, float screenHeight) {
        if (detection == null) return 3.0f;

        float frameArea = screenWidth * screenHeight;
        if (frameArea <= 0) return 3.0f;

        float areaRatio = detection.getArea() / frameArea;

        if (areaRatio > 0.30f) return 0.6f;
        if (areaRatio > 0.20f) return 0.9f;
        if (areaRatio > 0.12f) return 1.2f;
        if (areaRatio > 0.08f) return 1.6f;
        if (areaRatio > 0.05f) return 2.0f;
        if (areaRatio > 0.03f) return 2.6f;
        if (areaRatio > 0.015f) return 3.2f;
        return 4.0f;
    }

    private static String formatDistance(float distanceMeters) {
        return String.format(Locale.US, "%.1f meters", distanceMeters);
    }
}