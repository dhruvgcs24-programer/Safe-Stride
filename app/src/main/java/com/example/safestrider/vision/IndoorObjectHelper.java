package com.example.safestrider.vision;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class IndoorObjectHelper {

    private static final Set<String> DEFAULT_USEFUL_OBJECTS = new HashSet<>(Arrays.asList(
            "person",
            "chair",
            "couch",
            "dining table",
            "backpack",
            "cell phone"
    ));

    private static final Set<String> FUTURE_CUSTOM_OBJECTS = new HashSet<>(Arrays.asList(
            "stairs",
            "door",
            "exit",
            "lift",
            "elevator",
            "corridor",
            "ramp"
    ));

    public static boolean isUsefulIndoorObject(String label) {
        if (label == null) return false;

        String normalized = label.toLowerCase(Locale.US).trim();
        return DEFAULT_USEFUL_OBJECTS.contains(normalized)
                || FUTURE_CUSTOM_OBJECTS.contains(normalized);
    }

    public static String userFriendlyLabel(String label) {
        if (label == null) return "object";

        String normalized = label.toLowerCase(Locale.US).trim();

        if (normalized.equals("dining table")) return "table";
        if (normalized.equals("cell phone")) return "phone";
        if (normalized.equals("elevator")) return "lift";

        return normalized;
    }

    public static boolean isHighPriorityLabel(String label) {
        if (label == null) return false;

        String normalized = label.toLowerCase(Locale.US).trim();

        return normalized.equals("person")
                || normalized.equals("stairs")
                || normalized.equals("door")
                || normalized.equals("exit")
                || normalized.equals("lift")
                || normalized.equals("elevator");
    }
}